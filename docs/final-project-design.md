# Final Project Design

## Goal

Build a portfolio-grade Spring Boot backend for a reservation system.

The final project should show that the application can model a real booking
domain, expose a documented REST API, persist data safely, validate business
rules, and run predictably in local and CI environments.

## Position in Portfolio

This project follows:

- `studytracker`: basic Java CLI, storage and tests;
- `passwordmanager`: stronger CLI architecture, crypto, documentation and CI.

This project should add:

- Spring Boot REST API design;
- JPA persistence with relational constraints;
- transaction-aware business logic;
- PostgreSQL and Flyway migrations;
- integration tests with Testcontainers;
- Docker Compose local environment;
- OpenAPI documentation;
- portfolio-level README and demo flow.

## Product Scope

The system manages reservable offers and customer reservations.

Example domain: trips, tours, rooms, appointments or activities. The exact
business label can stay generic, but the domain should behave like a real
reservation backend.

## Core Use Cases

- Admin creates, updates, archives and lists offers.
- Admin defines availability slots for an offer.
- Customer lists active offers and available slots.
- Customer creates a reservation for an available slot.
- Customer can cancel a reservation.
- Admin can confirm or reject a reservation.
- The system prevents invalid reservations:
  - reservation for archived offer;
  - reservation for missing slot;
  - reservation above capacity;
  - reservation in the past;
  - invalid status transition.

Authentication can be deferred until V2. If added, keep it simple and explicit:
admin/customer roles with Spring Security and JWT or HTTP Basic for local demo.

## Final Architecture

Target package structure:

```text
com.github.wikor2115.reservation
  ReservationApplication
  offer
    api
    domain
    repository
    service
  availability
    api
    domain
    repository
    service
  reservation
    api
    domain
    repository
    service
  common
    api
    error
    time
```

Keep business rules in domain/service code, not in controllers.

Controllers should:

- accept request DTOs;
- validate input;
- call services;
- return response DTOs;
- avoid exposing JPA entities directly.

Services should:

- own transaction boundaries;
- load aggregates;
- enforce cross-entity rules;
- throw typed business exceptions.

Repositories should stay thin Spring Data interfaces unless a query genuinely
needs custom implementation.

## Domain Model

### Offer

Represents a reservable item.

Fields:

- `id`
- `name`
- `description`
- `imageUrl`
- `price`
- `archived`
- `createdAt`
- `updatedAt`

Rules:

- active offers are visible to customers;
- archived offers cannot receive new availability slots or reservations;
- price must be positive and have at most two decimal places;
- names and descriptions are trimmed and length-limited.

### AvailabilitySlot

Represents a time window and capacity for an offer.

Fields:

- `id`
- `offerId`
- `startsAt`
- `endsAt`
- `capacity`
- `reservedCount`
- `status`
- `createdAt`
- `updatedAt`

Statuses:

- `OPEN`
- `CLOSED`
- `CANCELLED`

Rules:

- `startsAt` must be before `endsAt`;
- capacity must be positive;
- slots cannot be created in the past;
- reservations cannot exceed capacity;
- closed/cancelled slots cannot accept reservations.

### Reservation

Represents a customer booking.

Fields:

- `id`
- `slotId`
- `customerName`
- `customerEmail`
- `partySize`
- `status`
- `createdAt`
- `updatedAt`

Statuses:

- `PENDING`
- `CONFIRMED`
- `REJECTED`
- `CANCELLED`

Rules:

- `partySize` must be positive;
- email must be valid;
- only pending reservations can be confirmed or rejected;
- confirmed and pending reservations consume capacity;
- cancellation releases capacity when applicable;
- repeated cancellation should return a clear business error.

## REST API Shape

Base path: `/api/v1`.

Offer endpoints:

```text
GET    /api/v1/offers
GET    /api/v1/offers/{offerId}
POST   /api/v1/admin/offers
PATCH  /api/v1/admin/offers/{offerId}
DELETE /api/v1/admin/offers/{offerId}
```

Availability endpoints:

```text
GET    /api/v1/offers/{offerId}/availability
POST   /api/v1/admin/offers/{offerId}/availability
PATCH  /api/v1/admin/availability/{slotId}
DELETE /api/v1/admin/availability/{slotId}
```

Reservation endpoints:

```text
POST   /api/v1/reservations
GET    /api/v1/reservations/{reservationId}
POST   /api/v1/reservations/{reservationId}/cancel
POST   /api/v1/admin/reservations/{reservationId}/confirm
POST   /api/v1/admin/reservations/{reservationId}/reject
```

Error response:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "details": [
    {
      "field": "customerEmail",
      "message": "must be a well-formed email address"
    }
  ]
}
```

## Persistence

Use PostgreSQL for normal local/dev runs.

Use H2 only for lightweight tests if needed. Prefer Testcontainers PostgreSQL
for integration tests that verify repository and API behavior.

Use Flyway migrations:

```text
src/main/resources/db/migration/V1__create_offers.sql
src/main/resources/db/migration/V2__create_availability_slots.sql
src/main/resources/db/migration/V3__create_reservations.sql
```

Avoid `spring.jpa.hibernate.ddl-auto=update` in the final project. Use
`validate` once migrations exist.

## Testing Strategy

Required test layers:

- domain unit tests for entity business rules;
- service tests for transaction and rule behavior;
- repository tests with PostgreSQL Testcontainers;
- API tests for status codes, validation errors and JSON responses;
- one full happy-path integration test:
  - create offer;
  - create slot;
  - create reservation;
  - confirm reservation;
  - verify remaining capacity;
  - cancel reservation.

CI should run:

```bash
./mvnw test
```

Later, when Docker-based integration tests are stable:

```bash
./mvnw verify
```

## Local Development

Final local flow:

```bash
docker compose up -d
./mvnw spring-boot:run
```

Useful URLs:

```text
http://localhost:8080/swagger-ui.html
http://localhost:8080/v3/api-docs
```

## Documentation

Final docs should include:

- `README.md` with project purpose, stack, setup, API examples and demo flow;
- `docs/final-project-design.md`;
- `docs/api-examples.md`;
- `docs/architecture.md`;
- `docs/testing.md`;
- optional terminal GIF or screenshots after the API flow is stable.

## Milestones

### Milestone 0: Cleanup

- Fix Maven Wrapper executable bit.
- Add root `.gitignore`.
- Remove tracked IDE files.
- Add README skeleton.
- Fix invalid tests.

### Milestone 1: Offer Module V1

- Complete offer CRUD.
- Keep request/response DTOs separate.
- Add API tests and service tests.
- Add consistent error responses.

Detailed execution checklist:
`docs/offer-module-v1-plan.md`.

### Milestone 2: Availability

- Add availability slots.
- Add capacity rules.
- Add migrations.
- Add API tests.

### Milestone 3: Reservations

- Add reservation creation.
- Prevent overbooking.
- Add cancellation and status transitions.
- Add integration tests for the full booking flow.

### Milestone 4: Production-Like Setup

- PostgreSQL through Docker Compose.
- Flyway migrations.
- Testcontainers.
- CI workflow.
- OpenAPI polish.

### Milestone 5: Portfolio Polish

- Final README.
- Demo flow.
- Example curl commands.
- Architecture diagram or concise architecture doc.
- Clear limitations and future work.

## Explicit Non-Goals for V1

- Frontend application.
- Online payments.
- Email sending.
- Calendar integration.
- Multi-tenant support.
- Complex authentication and authorization.
- Distributed locking or microservices.

These can be mentioned as future work, but they should not block the V1
portfolio backend.

## Definition of Done

The project is portfolio-ready when:

- clean clone can run with Maven Wrapper;
- tests pass in CI;
- PostgreSQL local environment works through Docker Compose;
- API is documented through README and OpenAPI;
- core reservation flow is covered by integration tests;
- business rules are visible in code and tests;
- README explains what the project demonstrates and what is intentionally out
  of scope.
