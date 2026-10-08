# Public HTTPS deployment

The complete stack can run on one Linux server with Docker Compose. [Caddy obtains and renews a publicly trusted TLS certificate automatically when the domain points to the server and ports 80/443 are reachable](https://caddyserver.com/docs/quick-starts/reverse-proxy). PostgreSQL stays inside the Compose network; only Caddy publishes public ports. The frontend remains bound to `127.0.0.1:8088` for local checks on the server.

## Prepare a server

Use a Linux server with enough memory to build and run PostgreSQL, four Java applications, Nginx, and Caddy. Install Docker Engine and the Compose plugin. Point a domain's A/AAAA record to the server, and allow incoming TCP 80/443. If a domain is unavailable, an IP-derived hostname from [sslip.io](https://nip.io/) can provide DNS for a short-lived demo, subject to that external DNS service remaining available.

Clone the repository. Run `./start-demo.sh` once so the script creates random secrets in the ignored `.env` file. Add `PUBLIC_DOMAIN=your.domain.example` to `.env`; keep that file on the server and out of Git. Review the admin and customer passwords in `.env`. Then run:

```bash
docker compose -f compose.yaml -f compose.public.yaml up --build --detach --wait
```

Open `https://your.domain.example/` from another network and check a customer booking, admin confirmation, customer cancellation, and the same page on a phone. The Python smoke check uses local HTTP on the server:

```bash
python3 scripts/smoke-real-api.py
```

For updates, pull the reviewed commit and run the same `docker compose ... up --build --detach --wait` command. The named PostgreSQL volume persists across container recreation. Make a database backup before upgrades and keep the `.env` file and Caddy data volume, which contains certificate state. The local `./start-demo.sh reset` command deletes the database and is intended only for disposable demos.

## Accounts, backup, and recovery

The generated customer account can create and cancel only its own reservations. The generated administrator account can create offers/slots and confirm or reject bookings. Share the administrator password only with trusted reviewers; neither password belongs in the repository or public README. Ask visitors to use sample names and email addresses.

Before an update, save the current commit ID and a PostgreSQL backup on the server:

```bash
git rev-parse HEAD
docker compose -f compose.yaml -f compose.public.yaml exec -T postgres pg_dump -U reservation -d reservation -Fc > reservation-backup.dump
```

If an update fails, check `docker compose -f compose.yaml -f compose.public.yaml ps` and `docker compose -f compose.yaml -f compose.public.yaml logs --since 10m` without sharing `.env` or bearer tokens. Re-checkout the previous commit and rebuild to roll back application code. If a database migration is incompatible with that commit, stop the four Java services, restore the backup with `pg_restore --clean --if-exists` into a disposable replacement database volume, and bring the previous commit back up. Practice the restore on a separate Compose project before relying on it for a live demo.

To reset a public demo, stop the combined Compose project, delete **only** its `reservation-data` volume, then run the combined `up --build --detach --wait` command again. This reseeds demo accounts, offer, and slot while preserving Caddy's `caddy-data` certificate volume. This destroys all reservations and accounts created since the previous reset; keep the backup first if needed. A disposable local demo can instead use `./start-demo.sh reset` with its explicit confirmation.

This repository prepares deployment but does not provision a server, domain, account, or billing. Record the final URL, hosting cost, and a phone test result in the README and portfolio summary after deployment.
