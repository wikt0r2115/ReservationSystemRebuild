# Reservation System Rebuild v1.0.0

This release is a locally run portfolio application. Reviewers install Docker with Docker Compose, download the source, and start the complete React, Spring Boot, and PostgreSQL stack on their own computer. No hosted demo is required.

## Try it

```bash
git clone --branch v1.0.0 --depth 1 https://github.com/wikt0r2115/ReservationSystemRebuild.git
cd ReservationSystemRebuild
./start-demo.sh
```

Open `http://localhost:8088`. The script generates customer and administrator passwords in the ignored local `.env` file. See the [README](https://github.com/wikt0r2115/ReservationSystemRebuild/blob/v1.0.0/README.md) for the complete booking, confirmation, and cancellation walkthrough, plus stop, update, and reset commands.

## Included

- Customer registration, login, offer and slot browsing, reservations, cancellation, and latest booking recovery after refresh.
- Administrator offer and slot creation, reservation confirmation and rejection.
- PostgreSQL transactions and JPA version checks for concurrency-safe capacity accounting; HTTP 409 for conflicts.
- One-command Docker Compose start, generated local credentials, health checks, sample data, persistent database, and explicit reset.
- Desktop and mobile Playwright tests against the real API, including authorization, capacity conflict, API outage, and lost write response.
- [Performance report](https://github.com/wikt0r2115/ReservationSystemRebuild/blob/v1.0.0/docs/performance.md) with p50/p95, throughput, resource samples, and query plans.

## Verification

- Full Java suite: 249 tests, 0 failures, 0 skipped.
- Clean Docker Compose start, real API smoke test, and 12 browser tests on desktop/mobile emulation.
- Load run: 5,400 measured HTTP requests, 0 errors; 1,800 reservations and 150 slots reconciled.
- [GitHub Actions CI](https://github.com/wikt0r2115/ReservationSystemRebuild/actions) runs the Java suite, frontend build, package, PostgreSQL smoke, and real API browser E2E.

The performance results describe a local test environment, not a hosted capacity guarantee. The mobile tests use browser emulation; the release is designed for local access on each reviewer's computer.
