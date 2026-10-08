#!/usr/bin/env bash
set -Eeuo pipefail

cd "$(dirname "$0")"

action="${1:-start}"
case "$action" in
  start|update|stop|reset) ;;
  *) echo "Usage: ./start-demo.sh [start|update|stop|reset]" >&2; exit 2 ;;
esac

if [[ "$action" == "reset" ]]; then
  echo "This deletes the local demo database volume. Type RESET to continue:"
  read -r confirmation
  [[ "$confirmation" == "RESET" ]] || exit 1
  docker compose down --volumes
  exit
fi

if [[ "$action" == "stop" ]]; then
  docker compose down
  exit
fi

if [[ ! -f .env ]]; then
  umask 077
  random_value() { od -An -N32 -tx1 /dev/urandom | tr -d ' \n'; }
  cat > .env <<EOF
FRONTEND_BIND=127.0.0.1
FRONTEND_PORT=8088
POSTGRES_PASSWORD=$(random_value)
JWT_SECRET=$(random_value)
DEMO_ADMIN_PASSWORD=$(random_value)
DEMO_CUSTOMER_PASSWORD=$(random_value)
EOF
fi

docker compose up --build --detach --wait

echo "Demo: http://localhost:${FRONTEND_PORT:-$(sed -n 's/^FRONTEND_PORT=//p' .env)}"
echo "Customer: jan@example.com"
echo "Admin: admin@example.com"
echo "Passwords are in the local .env file."
