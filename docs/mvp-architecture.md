# MVP Architecture

## Current Decision

The current backend is a modular Spring Boot MVP, not a finished production
microservice architecture.

It contains four runnable modules:

```text
reservation/auth
reservation/offer
reservation/availability
reservation/booking
```

Each module has its own Spring Boot application, local H2 configuration,
PostgreSQL/Flyway profile and tests. The Maven aggregator in
`reservation/pom.xml` builds and tests all modules together.

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
- pending reservations hold capacity until they are confirmed, rejected or
  cancelled;
- rejection and cancellation release capacity through
  `AvailabilitySlot.release(...)`;
- tests verify the real lifecycle at domain, service, repository and API level.

For a future microservice split:

- `booking` must not use `AvailabilitySlotRepository` directly;
- availability capacity changes should move behind an HTTP API, event contract
  or shared transactional boundary decision;
- each service should have its own database/migrations and clear integration
  tests around service boundaries.

## Current Known Gaps

- Authentication uses local JWT bearer tokens issued by the auth module.
- Admin/customer split exists, but there is no external identity provider,
  refresh-token flow or token revocation yet.
- H2 is still used for fast local/test runs.
- PostgreSQL/Flyway has a CI smoke job, and booking now has a focused
  Testcontainers PostgreSQL service test for the reservation lifecycle.
- OpenAPI is enabled only in dev profiles. Controllers include operation
  summaries and descriptions, but schemas/examples can still be expanded.

## Recommended Next Hardening Order

1. Watch the PostgreSQL smoke job on GitHub Actions and fix any environment-only
   failures.
2. Add OpenAPI schema examples and response examples.
3. Add more Testcontainers coverage for auth, offer and availability repository
   behavior where H2 differs from PostgreSQL.
4. Harden auth with refresh-token/revocation decisions if needed.
5. Decide whether to keep modular MVP style or split into real services.
