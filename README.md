# Reservation System Backend

Spring Boot backend for a reservation system. The project is currently in an
early refactor stage: the existing module manages offers, and the planned final
shape adds availability slots, reservations, PostgreSQL, migrations,
integration tests and Docker-based local development.

## Current Status

- Java 21
- Spring Boot 4.0.6
- Maven Wrapper
- Spring Web MVC
- Spring Data JPA
- Bean Validation
- H2 for the current local development setup
- OpenAPI UI through Springdoc

Current Maven modules:

```text
reservation/offer
reservation/availability
```

## Roadmap

The final project design is documented in
[docs/final-project-design.md](docs/final-project-design.md).

The detailed V1 plan for the first module is documented in
[docs/offer-module-v1-plan.md](docs/offer-module-v1-plan.md).

High-level milestones:

1. Repository cleanup and current offer module stabilization.
2. Complete offer CRUD with tests and consistent errors.
3. Add availability slots.
4. Add reservation creation, cancellation and status transitions.
5. Add PostgreSQL, Flyway, Testcontainers, Docker Compose and CI.
6. Polish README, API examples and demo flow for portfolio use.

## Requirements

- Java 21
- Git
- No local Maven installation required. Use Maven Wrapper.

## Build And Test

From the offer module:

```bash
cd reservation/offer
./mvnw test
```

From the availability module:

```bash
cd reservation/availability
./mvnw test
```

Run a single test class:

```bash
./mvnw -Dtest=OfferServiceTest test
```

Run a single test method:

```bash
./mvnw -Dtest=OfferServiceTest#createOffer_savesNewOffer test
```

Build the application:

```bash
./mvnw clean package
```

Run locally:

```bash
./mvnw spring-boot:run
```

The API starts on:

```text
http://localhost:8080
```

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

H2 console for the current development setup:

```text
http://localhost:8080/h2-console
```

## Current API

Public list active offers:

```bash
curl http://localhost:8080/api/v1/offers
```

Public get active offer by id:

```bash
curl http://localhost:8080/api/v1/offers/1
```

Admin list all offers (active + archived):

```bash
curl http://localhost:8080/api/v1/admin/offers
```

Admin get offer by id:

```bash
curl http://localhost:8080/api/v1/admin/offers/1
```

Admin create offer:

```bash
curl -X POST http://localhost:8080/api/v1/admin/offers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "City Tour",
    "imageUrl": "https://example.com/city.jpg",
    "description": "Full day city tour",
    "price": 199.99
  }'
```

Admin update offer (partial):

```bash
curl -X PATCH http://localhost:8080/api/v1/admin/offers/1 \
  -H "Content-Type: application/json" \
  -d '{
    "price": 219.99
  }'
```

Admin archive offer:

```bash
curl -X DELETE http://localhost:8080/api/v1/admin/offers/1
```

Note: in V1, admin endpoints are not authenticated yet.

## Notes

This is not portfolio-ready yet. The immediate goal is to stabilize the current
offer module, then evolve it into a complete reservation backend with a
production-like local setup and integration test coverage.
