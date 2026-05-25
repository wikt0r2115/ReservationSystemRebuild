# Reservation System Backend

Spring Boot backend for a reservation system. The current MVP contains three
tested modules: offers, availability slots and reservations.

The project is intentionally still a portfolio MVP. It uses H2 for local/dev
runs, has no authentication yet, and keeps production hardening items such as
PostgreSQL, migrations and Docker Compose as explicit next steps.

## Current Status

- Java 21
- Spring Boot 4.0.6
- Maven
- Spring Web MVC
- Spring Data JPA
- Bean Validation
- H2 for current local/dev/test setup
- OpenAPI UI through Springdoc in the `dev` profile

Current Maven modules:

```text
reservation/offer
reservation/availability
reservation/booking
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

## Run Locally

Run one module as an executable Spring Boot jar:

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

Swagger UI is enabled in the `dev` profile and disabled in default/test/prod:

```text
http://localhost:<port>/swagger-ui.html
```

H2 console is enabled in the `dev` profile:

```text
http://localhost:<port>/h2-console
```

## Smoke Test

The booking module has a dev-only seed availability slot so the reservation
flow can be tested through real HTTP requests.

Start booking with the `dev` profile:

```bash
cd reservation
mvn -pl booking -am -DskipTests package
java -jar booking/target/booking-0.0.1-SNAPSHOT-exec.jar --spring.profiles.active=dev
```

In another terminal:

```bash
curl -i -X POST http://localhost:8082/api/v1/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "availabilitySlotId": 1,
    "customerName": "Jan Kowalski",
    "customerEmail": "jan@example.com",
    "partySize": 2
  }'
```

```bash
curl -i http://localhost:8082/api/v1/reservations/1
```

```bash
curl -i -X DELETE http://localhost:8082/api/v1/reservations/1
```

For the full endpoint list and examples, see
[docs/api-contract.md](docs/api-contract.md).

## Current API Summary

Offer:

```text
GET    /api/v1/offers
GET    /api/v1/offers/{offerId}
GET    /api/v1/admin/offers
GET    /api/v1/admin/offers/{offerId}
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

Note: admin endpoints are not authenticated yet.

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
offer:        42 tests
availability: 64 tests
booking:      55 tests
BUILD SUCCESS

mvn -DskipTests package
plain module jars plus executable *-exec.jar artifacts
BUILD SUCCESS
```
