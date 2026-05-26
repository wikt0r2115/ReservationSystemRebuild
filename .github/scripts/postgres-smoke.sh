#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
RESERVATION_DIR="$ROOT_DIR/reservation"
LOG_DIR="${POSTGRES_SMOKE_LOG_DIR:-$ROOT_DIR/.ci-postgres-smoke}"

export DATABASE_URL="${DATABASE_URL:-jdbc:postgresql://localhost:5432/reservation}"
export DATABASE_USERNAME="${DATABASE_USERNAME:-reservation}"
export DATABASE_PASSWORD="${DATABASE_PASSWORD:-reservation}"
export JWT_SECRET="${JWT_SECRET:-ci-postgres-smoke-secret}"
export JWT_ISSUER="${JWT_ISSUER:-reservation-auth}"
export JWT_EXPIRATION="${JWT_EXPIRATION:-PT2H}"

mkdir -p "$LOG_DIR"
PIDS=()

cleanup() {
    local status=$?
    set +e
    for pid in "${PIDS[@]:-}"; do
        if kill -0 "$pid" 2>/dev/null; then
            kill "$pid" 2>/dev/null || true
        fi
    done
    for pid in "${PIDS[@]:-}"; do
        wait "$pid" 2>/dev/null || true
    done
    exit "$status"
}
trap cleanup EXIT

tail_log() {
    local log_file="$1"
    if [[ -f "$log_file" ]]; then
        tail -n 120 "$log_file"
    fi
}

wait_for_startup() {
    local name="$1"
    local pid="$2"
    local log_file="$3"

    for _ in $(seq 1 60); do
        if grep -q "Started .*Application" "$log_file" 2>/dev/null; then
            echo "$name started"
            return 0
        fi
        if grep -q "APPLICATION FAILED TO START" "$log_file" 2>/dev/null; then
            echo "$name failed during startup"
            tail_log "$log_file"
            return 1
        fi
        if ! kill -0 "$pid" 2>/dev/null; then
            echo "$name exited during startup"
            tail_log "$log_file"
            return 1
        fi
        sleep 2
    done

    echo "$name did not start before timeout"
    tail_log "$log_file"
    return 1
}

start_service() {
    local name="$1"
    local jar_path="$RESERVATION_DIR/$2"
    local log_file="$LOG_DIR/$name.log"

    if [[ ! -f "$jar_path" ]]; then
        echo "Missing jar: $jar_path"
        return 1
    fi

    echo "Starting $name"
    (cd "$RESERVATION_DIR" && exec java -jar "$jar_path" --spring.profiles.active=dev-postgres) \
        >"$log_file" 2>&1 &
    local pid=$!
    PIDS+=("$pid")
    wait_for_startup "$name" "$pid" "$log_file"
}

assert_status() {
    local label="$1"
    local expected="$2"
    local actual="$3"
    local response_file="$4"

    if [[ "$actual" != "$expected" ]]; then
        echo "$label returned HTTP $actual, expected $expected"
        if [[ -f "$response_file" ]]; then
            cat "$response_file"
        fi
        return 1
    fi
}

start_service "auth" "auth/target/auth-0.0.1-SNAPSHOT-exec.jar"
start_service "offer" "offer/target/offer-0.0.1-SNAPSHOT-exec.jar"
start_service "availability" "availability/target/availability-0.0.1-SNAPSHOT-exec.jar"
start_service "booking" "booking/target/booking-0.0.1-SNAPSHOT-exec.jar"

smoke_email="${SMOKE_EMAIL:-smoke-${GITHUB_RUN_ID:-local}-$(date +%s)@example.com}"

register_response="$LOG_DIR/register.json"
register_status="$(curl -sS -o "$register_response" -w "%{http_code}" \
    -X POST "http://localhost:8083/api/v1/auth/register" \
    -H "Content-Type: application/json" \
    --data "{\"displayName\":\"Smoke Customer\",\"email\":\"$smoke_email\",\"password\":\"customer123\"}")"
assert_status "register" "201" "$register_status" "$register_response"

login_response="$LOG_DIR/login.json"
login_status="$(curl -sS -o "$login_response" -w "%{http_code}" \
    -X POST "http://localhost:8083/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    --data "{\"email\":\"$smoke_email\",\"password\":\"customer123\"}")"
assert_status "login" "200" "$login_status" "$login_response"

token="$(sed -n 's/.*"token":"\([^"]*\)".*/\1/p' "$login_response")"
if [[ -z "$token" ]]; then
    echo "login response did not contain token"
    cat "$login_response"
    exit 1
fi

reservation_response="$LOG_DIR/create-reservation.json"
reservation_status="$(curl -sS -o "$reservation_response" -w "%{http_code}" \
    -X POST "http://localhost:8082/api/v1/reservations" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $token" \
    --data "{\"availabilitySlotId\":1,\"customerName\":\"Smoke Customer\",\"customerEmail\":\"$smoke_email\",\"partySize\":1}")"
assert_status "create reservation" "201" "$reservation_status" "$reservation_response"

echo "PostgreSQL smoke passed"
