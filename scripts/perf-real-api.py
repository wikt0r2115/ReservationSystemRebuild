#!/usr/bin/env python3
"""Reproducible local HTTP load run against the complete demo stack."""

import argparse
import concurrent.futures
import json
import random
import statistics
import subprocess
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timedelta, timezone
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def secrets():
    return dict(line.split("=", 1) for line in (ROOT / ".env").read_text().splitlines()
                if "=" in line and not line.startswith("#"))


def call(base, method, path, token=None, data=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    request = urllib.request.Request(
        base + path,
        data=json.dumps(data).encode() if data is not None else None,
        headers=headers,
        method=method,
    )
    started = time.perf_counter()
    try:
        with urllib.request.urlopen(request, timeout=15) as response:
            content = response.read()
            return response.status, json.loads(content) if content else None, (time.perf_counter() - started) * 1000
    except urllib.error.HTTPError as error:
        return error.code, error.read().decode(errors="replace"), (time.perf_counter() - started) * 1000


def checked(base, method, path, token=None, data=None, expected=200):
    status, body, elapsed = call(base, method, path, token, data)
    if status != expected:
        raise RuntimeError(f"{method} {path}: HTTP {status}: {str(body)[:200]}")
    return body, elapsed


def measure(name, count, workers, action):
    started = time.perf_counter()
    with concurrent.futures.ThreadPoolExecutor(max_workers=workers) as pool:
        outcomes = list(pool.map(action, range(count)))
    duration = time.perf_counter() - started
    latencies = sorted(item[0] for item in outcomes)
    statuses = [item[1] for item in outcomes]
    result = {
        "scenario": name,
        "concurrency": workers,
        "requests": count,
        "seconds": round(duration, 3),
        "throughput_rps": round(count / duration, 2),
        "p50_ms": round(statistics.median(latencies), 2),
        "p95_ms": round(latencies[max(0, int(0.95 * (len(latencies) - 1)))], 2),
        "errors": sum(status >= 400 for status in statuses),
        "status_counts": {str(status): statuses.count(status) for status in sorted(set(statuses))},
    }
    return result, outcomes


def sample_containers(project, stop, samples):
    names = [f"{project}-{service}-1" for service in ("postgres", "auth", "offer", "availability", "booking", "frontend")]
    while not stop.is_set():
        try:
            stats = subprocess.run(
                ["docker", "stats", "--no-stream", "--format", "{{.Name}}|{{.CPUPerc}}|{{.MemUsage}}", *names],
                capture_output=True, text=True, timeout=10, check=True,
            )
            connections = subprocess.run(
                ["docker", "exec", names[0], "psql", "-U", "reservation", "-d", "reservation", "-Atc",
                 "select count(*) from pg_stat_activity where datname = 'reservation'"],
                capture_output=True, text=True, timeout=10, check=True,
            )
            samples.append({"containers": stats.stdout.strip().splitlines(),
                            "db_connections": int(connections.stdout.strip())})
        except (subprocess.SubprocessError, ValueError):
            pass
        stop.wait(2)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default="http://127.0.0.1:8088")
    parser.add_argument("--offers", type=int, default=30)
    parser.add_argument("--slots-per-offer", type=int, default=5)
    parser.add_argument("--requests", type=int, default=150)
    parser.add_argument("--concurrency", type=int, nargs="+", default=[10, 25, 50])
    parser.add_argument("--docker-project", help="sample CPU, memory and PostgreSQL connections during load")
    args = parser.parse_args()
    if args.offers < 1 or args.slots_per_offer < 1 or args.requests < 1 or any(n < 1 for n in args.concurrency):
        parser.error("all counts must be positive")
    base = args.base_url.rstrip("/")
    credentials = secrets()
    admin, _ = checked(base, "POST", "/auth-api/api/v1/auth/login", data={
        "email": "admin@example.com", "password": credentials["DEMO_ADMIN_PASSWORD"]})
    customer, _ = checked(base, "POST", "/auth-api/api/v1/auth/login", data={
        "email": "jan@example.com", "password": credentials["DEMO_CUSTOMER_PASSWORD"]})
    admin_token, customer_token = admin["token"], customer["token"]

    run_id = datetime.now(timezone.utc).strftime("%Y%m%d%H%M%S")
    slot_ids = []
    starts = datetime(2031, 1, 1, 10, tzinfo=timezone.utc)
    for offer_index in range(args.offers):
        offer, _ = checked(base, "POST", "/offer-api/api/v1/admin/offers", admin_token, {
            "name": f"Performance {run_id} #{offer_index}",
            "imageUrl": "https://example.com/performance-placeholder.svg",
            "description": "Synthetic load test offer",
            "price": 100,
        }, expected=201)
        for slot_index in range(args.slots_per_offer):
            start = starts + timedelta(days=offer_index * args.slots_per_offer + slot_index)
            slot, _ = checked(base, "POST", f"/availability-api/api/v1/admin/offers/{offer['id']}/availability", admin_token, {
                "startsAt": start.replace(tzinfo=None).isoformat(),
                "endsAt": (start + timedelta(hours=2)).replace(tzinfo=None).isoformat(),
                "capacity": 40,
            }, expected=201)
            slot_ids.append(slot["id"])

    random.seed(42)
    offer_ids = [item["id"] for item in checked(base, "GET", "/offer-api/api/v1/offers")[0]
                 if item["name"].startswith(f"Performance {run_id}")]
    paths = [f"/availability-api/api/v1/offers/{offer_id}/availability" for offer_id in offer_ids]
    checked(base, "GET", "/offer-api/api/v1/offers")
    for _ in range(20):
        checked(base, "GET", random.choice(paths))

    results = []
    samples = []
    stop = threading.Event()
    sampler = None
    if args.docker_project:
        sampler = threading.Thread(target=sample_containers, args=(args.docker_project, stop, samples), daemon=True)
        sampler.start()
    all_created_ids = []
    try:
      for workers in args.concurrency:
        read, _ = measure("GET /offers", args.requests, workers,
                          lambda _: (lambda result: (result[2], result[0]))(call(base, "GET", "/offer-api/api/v1/offers")))
        results.append(read)
        availability, _ = measure("GET /availability", args.requests, workers,
                                  lambda index: (lambda result: (result[2], result[0]))(call(base, "GET", paths[index % len(paths)])))
        results.append(availability)

        def book(index):
            status, body, elapsed = call(base, "POST", "/booking-api/api/v1/reservations", customer_token, {
                "availabilitySlotId": slot_ids[index % len(slot_ids)],
                "customerName": "Performance Customer",
                "customerEmail": "jan@example.com",
                "partySize": 1,
            })
            return elapsed, status, body["id"] if status == 201 else None

        writes, outcomes = measure("POST /reservations", args.requests, workers, book)
        results.append(writes)
        created_ids = [item[2] for item in outcomes if item[2] is not None]
        all_created_ids.extend(created_ids)
        with concurrent.futures.ThreadPoolExecutor(max_workers=workers) as pool:
            cleanup = list(pool.map(lambda reservation_id: call(base, "DELETE", f"/booking-api/api/v1/reservations/{reservation_id}", customer_token), created_ids))
        if any(status != 200 for status, _, _ in cleanup):
            raise RuntimeError("reservation cleanup failed")
    finally:
        stop.set()
        if sampler:
            sampler.join(timeout=12)

    query = urllib.parse.urlencode({"customerEmail": "jan@example.com"})
    reservations, _ = checked(base, "GET", f"/booking-api/api/v1/reservations?{query}", customer_token)
    status_by_id = {item["id"]: item["status"] for item in reservations}
    if any(status_by_id.get(reservation_id) != "CANCELLED" for reservation_id in all_created_ids):
        raise RuntimeError("created reservations were not all cancelled")
    tested_slot_ids = set(slot_ids)
    for offer_id in offer_ids:
        slots, _ = checked(base, "GET", f"/availability-api/api/v1/admin/offers/{offer_id}/availability", admin_token)
        if any(slot["reservedCount"] != 0 or slot["reservedCount"] > slot["capacity"]
               for slot in slots if slot["id"] in tested_slot_ids):
            raise RuntimeError(f"capacity mismatch for offer {offer_id}")

    print(json.dumps({
        "timestamp_utc": datetime.now(timezone.utc).isoformat(),
        "base_url": base,
        "dataset": {"offers": args.offers, "slots": len(slot_ids), "slot_capacity": 40},
        "requests_per_scenario": args.requests,
        "resource_samples": samples,
        "consistency": {"cancelled_reservations_verified": len(all_created_ids),
                        "slots_with_zero_reserved_count": len(slot_ids)},
        "results": results,
    }, indent=2))


if __name__ == "__main__":
    main()
