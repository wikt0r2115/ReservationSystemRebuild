# MVP Architecture

## Current Decision

The current backend is a modular Spring Boot MVP, not a finished production
microservice architecture.

It contains three runnable modules:

```text
reservation/offer
reservation/availability
reservation/booking
```

Each module has its own Spring Boot application, local H2 configuration and
tests. The Maven aggregator in `reservation/pom.xml` builds and tests all
modules together.

## Booking And Availability Boundary

The booking module depends on the availability module at code level:

- it scans `AvailabilitySlot` as a JPA entity;
- it scans `AvailabilitySlotRepository`;
- `ReservationService` reserves and releases slot capacity in the same
  transaction as reservation creation/cancellation.

This is a pragmatic MVP tradeoff. It gives a consistent booking transaction and
keeps the portfolio implementation small enough to review.

## What This Implies

For the current MVP:

- `booking` owns the reservation flow;
- `availability` owns slot behavior such as capacity, status and reserved count;
- overbooking is prevented by `AvailabilitySlot.reserve(...)`;
- cancellation releases capacity through `AvailabilitySlot.release(...)`;
- tests verify the real lifecycle at domain, service, repository and API level.

For a future microservice split:

- `booking` must not use `AvailabilitySlotRepository` directly;
- availability capacity changes should move behind an HTTP API, event contract
  or shared transactional boundary decision;
- each service should have its own database/migrations and clear integration
  tests around service boundaries.

## Current Known Gaps

- No authentication or role enforcement.
- Admin paths are naming convention only.
- H2 is used for local/dev/test.
- No Flyway/Liquibase migrations yet.
- No Docker Compose or PostgreSQL profile yet.
- OpenAPI is enabled only in dev profiles and not yet curated with descriptions.

## Recommended Next Hardening Order

1. Add Flyway migrations.
2. Add PostgreSQL and Docker Compose for local development.
3. Add Spring Security with simple admin/customer split.
4. Add OpenAPI descriptions and example requests.
5. Decide whether to keep modular MVP style or split into real services.
