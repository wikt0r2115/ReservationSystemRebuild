# v1.0.0 — draft release notes

Reservation System Rebuild provides a complete customer booking and administrator workflow with PostgreSQL, Spring Boot, React, and Docker Compose.

## Included

- One-command local start with generated credentials, health checks, sample offer and slot, persistent database, and explicit reset.
- Customer registration, login, reservation, cancellation, and latest booking recovery after refresh.
- Administrator offer and slot creation, reservation confirmation and rejection.
- Concurrency-safe capacity accounting with PostgreSQL tests and HTTP 409 conflicts.
- Desktop and mobile Playwright scenarios against the real API, including authorization, capacity conflict, API outage, and lost write response.
- CI for Maven tests, frontend build, PostgreSQL smoke, and browser E2E.
- Performance script and [measured results](performance.md).

## Verification before publishing

- [x] Full local Java suite: 249 tests, 0 failures, 0 skipped.
- [x] Clean Compose start and real API browser flow: 12 tests passed on desktop/mobile emulation.
- [x] Load run: 5,400 measured HTTP requests, 0 errors; 1,800 reservations and 150 slots reconciled.
- [ ] GitHub Actions run on the release commit is green.
- [ ] Public HTTPS demo URL and cellular phone check are recorded.
- [ ] Release tag `v1.0.0` points to that checked commit.

The release should be published only after the remaining checks are complete.
