# Reservation System

Spring Boot backend and React frontend for a reservation system. The current
MVP contains three tested backend modules: offers, availability slots and
reservations. The frontend provides a small operations console for creating
offers/slots, making reservations and reviewing admin data.

The project is intentionally still a portfolio MVP. It keeps H2 for fast
local/test runs, and also has a PostgreSQL/Flyway/Docker Compose development
path plus simple HTTP Basic authentication for customer/admin flows.

## Current Status

- Java 21
- Spring Boot 4.0.6
- Maven
- Spring Web MVC
- Spring Data JPA
- Bean Validation
- H2 for current local/dev/test setup
- PostgreSQL dev profile with Flyway migrations
- Docker Compose for local PostgreSQL
- Spring Security HTTP Basic for customer/admin separation
- OpenAPI UI through Springdoc in the `dev` profile
- React 19 + Vite + TypeScript frontend

Current Maven modules:

```text
reservation/offer
reservation/availability
reservation/booking
```

Frontend app:

```text
frontend
```

## Documentation

- Final project direction: [docs/final-project-design.md](docs/final-project-design.md)
- Offer V1 plan: [docs/offer-module-v1-plan.md](docs/offer-module-v1-plan.md)
- Implemented MVP API: [docs/api-contract.md](docs/api-contract.md)
- Current architecture decision: [docs/mvp-architecture.md](docs/mvp-architecture.md)

## Build And Test

Run the full reactor from the aggregator:

```bash
cd reservation
mvn test
```

Run a focused module with its dependencies:

```bash
mvn test -pl booking -am
```

Run a single test class:

```bash
mvn -pl booking -Dtest=ReservationServiceTest test
```

Run a single test method:

```bash
mvn -pl booking -Dtest=ReservationServiceTest#createReservation_reservesAvailabilitySlotAndSavesReservation test
```

Build all modules:

```bash
mvn clean package
```

Build the frontend:

```bash
cd frontend
npm install
npm run build
```

## Run Locally

Run one module with the default H2 profile as an executable Spring Boot jar:

```bash
cd reservation
mvn -pl booking -am -DskipTests package
java -jar booking/target/booking-0.0.1-SNAPSHOT-exec.jar
```

The `-am` flag builds required module dependencies first. The executable jar
uses the `exec` classifier so the plain module jar remains usable as a Maven
dependency by other modules.

Default module ports:

```text
offer:        http://localhost:8080
availability: http://localhost:8081
booking:      http://localhost:8082
```

Run the frontend development server:

```bash
cd frontend
npm install
npm run dev
```

The Vite dev server starts on:

```text
http://localhost:5173
```

During local development the frontend proxies API traffic through these
prefixes, so the backend modules should be running on their default ports:

```text
/offer-api        -> http://localhost:8080
/availability-api -> http://localhost:8081
/booking-api      -> http://localhost:8082
```

For the complete browser flow across all three backend apps, run the modules
against the shared PostgreSQL database with `--spring.profiles.active=dev-postgres`.
The default H2 setup is useful for isolated module work, but each app gets its
own in-memory database, so data created through `availability` is not shared
with `booking`.

Frontend customer credentials can be overridden with `VITE_CUSTOMER_USERNAME`
and `VITE_CUSTOMER_PASSWORD`. Admin credentials are entered in the UI.

Swagger UI is enabled in the `dev` profile and disabled in default/test/prod:

```text
http://localhost:<port>/swagger-ui.html
```

H2 console is enabled in the `dev` profile:

```text
http://localhost:<port>/h2-console
```

## Run With PostgreSQL

Start local PostgreSQL:

```bash
cd reservation
docker compose up -d postgres
```

Build the modules:

```bash
mvn -DskipTests package
```

Run a module with Flyway migrations and PostgreSQL:

```bash
java -jar booking/target/booking-0.0.1-SNAPSHOT-exec.jar --spring.profiles.active=dev-postgres
```

The default local PostgreSQL settings are:

```text
url:      jdbc:postgresql://localhost:5432/reservation
username: reservation
password: reservation
```

They can be overridden with `DATABASE_URL`, `DATABASE_USERNAME` and
`DATABASE_PASSWORD`.

Default local API users:

```text
admin:    admin / admin123
customer: customer / customer123
```

## Smoke Test

The booking module has a dev-only seed availability slot so the reservation
flow can be tested through real HTTP requests. The same flow works with
`dev-postgres`; use `-u customer:customer123` for reservation endpoints and
`-u admin:admin123` for admin endpoints.

Start booking with the `dev` profile:

```bash
cd reservation
mvn -pl booking -am -DskipTests package
java -jar booking/target/booking-0.0.1-SNAPSHOT-exec.jar --spring.profiles.active=dev
```

In another terminal:

```bash
curl -i -X POST http://localhost:8082/api/v1/reservations \
  -u customer:customer123 \
  -H "Content-Type: application/json" \
  -d '{
    "availabilitySlotId": 1,
    "customerName": "Jan Kowalski",
    "customerEmail": "jan@example.com",
    "partySize": 2
  }'
```

```bash
curl -i -u customer:customer123 http://localhost:8082/api/v1/reservations/1
```

```bash
curl -i -X DELETE -u customer:customer123 http://localhost:8082/api/v1/reservations/1
```

For the full endpoint list and examples, see
[docs/api-contract.md](docs/api-contract.md).

## Current API Summary

Offer:

```text
GET    /api/v1/offers
GET    /api/v1/offers/{offerId}
GET    /api/v1/admin/offers
POST   /api/v1/admin/offers
PATCH  /api/v1/admin/offers/{offerId}
DELETE /api/v1/admin/offers/{offerId}
```

Availability:

```text
GET    /api/v1/offers/{offerId}/availability
POST   /api/v1/admin/offers/{offerId}/availability
PATCH  /api/v1/admin/availability/{slotId}
DELETE /api/v1/admin/availability/{slotId}
```

Booking:

```text
POST   /api/v1/reservations
GET    /api/v1/reservations/{reservationId}
GET    /api/v1/reservations?customerEmail=...
GET    /api/v1/admin/reservations
GET    /api/v1/admin/availability/{slotId}/reservations
DELETE /api/v1/reservations/{reservationId}
```

Admin endpoints require HTTP Basic credentials with the `ADMIN` role. Booking
reservation endpoints require the `CUSTOMER` or `ADMIN` role. Public offer and
availability read endpoints remain unauthenticated.

## MVP Architecture

The current backend should be treated as a modular Spring Boot MVP with three
separate runnable modules. Booking directly depends on the availability module
and uses its JPA entity/repository to reserve and release slot capacity in one
transaction.

This is acceptable for the current MVP and tests. If the project evolves into
separate production microservices, booking must stop directly using
availability repositories and switch to an HTTP/event/contract boundary.

## Current Verification

Latest local full reactor result:

```text
mvn test
offer:        46 tests
availability: 72 tests
booking:      59 tests
BUILD SUCCESS

mvn -DskipTests package
plain module jars plus executable *-exec.jar artifacts
BUILD SUCCESS
```
