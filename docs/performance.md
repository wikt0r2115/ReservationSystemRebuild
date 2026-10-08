# Local performance check

Measured on 2026-10-08 against a fresh `reservation-perf` Docker Compose project: Java 21, Spring Boot 4.0.6, PostgreSQL 16, Docker Engine 29.8.2, Compose 5.5.1. The HTTP client and all six containers ran on the same laptop (AMD Ryzen 7 3700U, 4 cores/8 threads, 13.6 GiB RAM). No network latency, TLS, or external users were involved. These are local reference numbers, not a production capacity promise.

## Reproduce

```bash
COMPOSE_PROJECT_NAME=reservation-perf FRONTEND_PORT=8090 ./start-demo.sh
python3 scripts/perf-real-api.py --base-url http://127.0.0.1:8090 --requests 600 --docker-project reservation-perf > docs/performance-results.json
docker compose -p reservation-perf down --volumes
```

The script logs in with the locally generated demo credentials, creates 30 synthetic offers and 150 slots (capacity 40 each), warms up the reads, and measures three request types at 10, 25, and 50 client threads. Each scenario sends 600 requests without a rate limit. Successful bookings are cancelled after each write batch so slot capacity is released. The final database contained 31 offers, 151 slots, and 1,800 cancelled reservations. Failed requests are counted and reported, never discarded from the latency sample. Raw numbers and resource samples are in [performance-results.json](performance-results.json).

| Request | Clients | p50 | p95 | Throughput | Errors |
| --- | ---: | ---: | ---: | ---: | ---: |
| GET offers | 10 | 15.49 ms | 38.60 ms | 529/s | 0/600 |
| GET offers | 25 | 30.74 ms | 58.27 ms | 690/s | 0/600 |
| GET offers | 50 | 42.53 ms | 92.92 ms | 726/s | 0/600 |
| GET availability | 10 | 13.86 ms | 24.10 ms | 660/s | 0/600 |
| GET availability | 25 | 29.29 ms | 52.28 ms | 740/s | 0/600 |
| GET availability | 50 | 46.20 ms | 88.48 ms | 788/s | 0/600 |
| POST reservation | 10 | 26.51 ms | 39.10 ms | 288/s | 0/600 |
| POST reservation | 25 | 46.23 ms | 72.57 ms | 509/s | 0/600 |
| POST reservation | 50 | 73.86 ms | 175.74 ms | 505/s | 0/600 |

Four `docker stats --no-stream` samples during the run showed booking CPU up to 221%, offer up to 305%, availability up to 437%, and PostgreSQL up to 54%. Reported CPU percentages can exceed 100% on a multicore host. Peak sampled memory was about 361 MiB auth, 378 MiB offer, 365 MiB availability, 351 MiB booking, 108 MiB PostgreSQL, and 10 MiB Nginx. PostgreSQL reported 41 connections in every sample. The short run and 2-second sampling interval can miss peaks; the resource figures are approximate.

After all requests, the script read back and verified all 1,800 created reservations as `CANCELLED` and all 150 measured slots with `reservedCount = 0` and capacity intact.

## Query plans and change

Before the change, the case-insensitive customer reservation lookup used a sequential scan on 2,250 reservations; `EXPLAIN (ANALYZE, BUFFERS)` showed 2.921 ms execution. The old index on `customer_email` could not serve `lower(customer_email)`. Reservations and lookup inputs now normalize email to lowercase. [Migration V3](../reservation/booking/src/main/resources/db/migration/booking/V3__index_customer_reservations.sql) normalizes stored addresses and replaces the old index with `(customer_email, created_at DESC)`, a form supported by PostgreSQL and H2.

After the migration and `ANALYZE`, a selective customer lookup used `Index Scan using idx_reservation_customer_email_created` with 0.051 ms execution and two shared buffer hits on 1,800 reservations. That plan looked up an absent email to demonstrate selectivity, so the time is not a direct before/after benchmark for an equally frequent customer. The popular demo account still may get a sequential scan when that is cheaper.

Open availability for one offer used the existing unique index on offer and slot times over 151 slots with 0.084 ms execution. Repeat the measurement with production-like data distribution and remote clients before setting service-level targets.

The three measured read paths each call one Spring Data repository method and map scalar fields from a single entity type. Those entities have no JPA relationships, so the reviewed paths cannot trigger a lazy-relationship N+1 query. Booking writes read and update a slot plus a reservation in one transaction; their SQL count is higher and the endpoint was measured separately. This is a code-path review, not a database-wide SQL trace.

## Readiness and diagnosis

Compose checks PostgreSQL with `pg_isready`, the API containers through their HTTP ports, and Nginx through its local HTTP port. `./start-demo.sh` waits for those checks and for seed completion. For a running stack, use `docker compose ps`, `docker stats --no-stream`, and `docker compose logs --since 10m booking availability postgres`. The performance script samples `pg_stat_activity` connection count without printing credentials. Keep `.env`, bearer tokens, and request Authorization headers out of shared diagnostics.
