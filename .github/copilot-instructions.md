# Copilot Instructions for ReservationSystemRebuild

## Build, test, and run commands

This repository currently has one active Maven module:

```bash
cd reservation/offer
```

Use Maven Wrapper commands from that directory:

```bash
./mvnw test
./mvnw -Dtest=OfferServiceTest test
./mvnw -Dtest=OfferServiceTest#createOffer_savesNewOffer test
./mvnw -Dtest=OfferControllerTest#createOffer_validRequest_returnsCreatedOffer test
./mvnw clean package
./mvnw spring-boot:run
```

There is no dedicated lint task configured in `reservation/offer/pom.xml` right now.
There is also no root multi-module Maven build; run commands from `reservation/offer`.

## High-level architecture

Current implemented architecture is a single Spring Boot module (`reservation/offer`) with layered packages:

- `api`: REST controller (`OfferController`), request/response records, and `GlobalExceptionHandler`
- `service`: transactional application logic (`OfferService`)
- `domain`: JPA entity with core business invariants (`Offer`)
- `repository`: Spring Data JPA interface (`OfferRepository`)

Current runtime flow:

1. HTTP requests hit `/offers` endpoints in `OfferController`
2. DTOs are validated with Jakarta Validation (`CreateOfferRequest`)
3. Service methods apply transaction boundaries and orchestration
4. Domain methods enforce rules (trimmed fields, price constraints, archived-state guards)
5. Repository persists/query entities (`findByArchivedFalse` for public listing)
6. Exceptions are mapped to stable API payloads through `GlobalExceptionHandler`

Roadmap context from `README.md` and `docs/final-project-design.md`: this module is V1 of a larger reservation backend that will add `availability` and `reservation` modules, versioned API structure (`/api/v1`), PostgreSQL/Flyway, and broader integration testing.

Runtime config conventions from `src/main/resources/application.properties`:

- in-memory H2 datasource for local/dev
- `spring.jpa.hibernate.ddl-auto=update` (current transitional state)
- `spring.jpa.open-in-view=false`
- H2 console enabled at `/h2-console`

## Key codebase conventions

- Keep business rule enforcement in domain/service code, not in controllers.
- Keep controllers DTO-based; do not expose entities directly in API responses.
- `Offer` uses an `archived` flag as a core state rule:
  - archived offers are excluded from public list queries (`findByArchivedFalse`)
  - archived offers cannot be mutated (`archive`, `rename`, `changePrice`, etc. guard on archived state)
- Domain validation style is explicit and exception-driven (`IllegalArgumentException` / `IllegalStateException`) with specific messages; service-layer missing entity uses typed exception (`OfferNotFoundException`).
- API error contract is centralized in `GlobalExceptionHandler` and `ApiErrorResponse` (currently `error` + `message`), with `NOT_FOUND` and `BAD_REQUEST` style error codes.
- Validation error handling currently returns only the first field error message (not a list of all field errors).
- Existing API tests use standalone MockMvc + mocked service (`OfferControllerTest`) instead of booting full Spring context for controller behavior.
- Existing service tests use Mockito unit tests (`OfferServiceTest`) and verify repository interactions directly.
- Treat `docs/final-project-design.md` and `docs/offer-module-v1-plan.md` as architecture/roadmap guidance; align code changes and README updates with those docs when extending behavior.
