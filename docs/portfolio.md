# Portfolio summary

Reservation System Rebuild is a full-stack booking application with a customer flow and an administrator workspace. A customer can register, browse offers and time slots, reserve places, review the latest booking after a refresh, and cancel it. An administrator can create offers and slots, then confirm or reject bookings. The default one-command Docker demo uses the real APIs and PostgreSQL.

## What this project demonstrates

- Java 21, Spring Boot, Spring Security/JWT, JPA, PostgreSQL, and Flyway across four runnable backend applications.
- React, TypeScript, and an Nginx reverse proxy with same-origin API paths.
- Atomic reservation/capacity updates with JPA version checking, HTTP 409 on a full slot, and PostgreSQL concurrency tests with several clients.
- Docker Compose setup with health checks, idempotent seed data, and generated local secrets.
- CI gates for Java tests, frontend build, PostgreSQL smoke flow, and desktop/mobile Playwright tests against the real API.
- [Reproducible local performance results](performance.md) with p50/p95, throughput, errors, resource samples, and query plans.

## Tradeoffs and limits

Booking imports the availability JPA module so both use one database transaction. This keeps seat accounting simple for the portfolio MVP; extracting independent services would require a different transaction boundary and recovery design. The customer UI currently shows the most recent booking rather than a paginated history. The load results come from one laptop with loopback traffic and should not be treated as hosted capacity. Reviewers clone the repository and run the complete app locally with Docker Compose; no public hosting is part of this release.

## Short CV text

**English:** Built a full-stack reservation system with Spring Boot, React, PostgreSQL, and Docker Compose. Implemented JWT authorization, concurrency-safe seat allocation, Flyway migrations, real-API Playwright tests, CI, and reproducible load measurements.

**Polski:** Zbudowałem aplikację do rezerwacji w Spring Boot, React i PostgreSQL. Wdrożyłem autoryzację JWT, bezpieczne współbieżnie przydzielanie miejsc, migracje Flyway, testy Playwright na prawdziwym API, CI oraz powtarzalne pomiary wydajności.

## Evidence

- [Customer booking screenshot](screenshots/customer-booking.png)
- [Admin workspace screenshot](screenshots/admin-workspace.png)
- [Mobile booking screenshot](screenshots/mobile-booking.png)
- [API contract](api-contract.md)
- [Architecture decisions](mvp-architecture.md)
- [Performance report](performance.md)
