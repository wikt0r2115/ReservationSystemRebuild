#!/usr/bin/env python3
"""Check the running Compose demo through the same HTTP origin as the browser."""

import json
import os
from pathlib import Path
from urllib.error import HTTPError
from urllib.request import Request, urlopen


BASE = os.environ.get("DEMO_BASE_URL", "http://localhost:8088").rstrip("/")


def env_values():
    values = {}
    for line in Path(__file__).resolve().parents[1].joinpath(".env").read_text().splitlines():
        if "=" in line and not line.startswith("#"):
            key, value = line.split("=", 1)
            values[key] = value
    return values


def request(method, path, payload=None, token=None):
    headers = {}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if payload is not None:
        headers["Content-Type"] = "application/json"
    data = json.dumps(payload).encode() if payload is not None else None
    with urlopen(Request(BASE + path, data=data, headers=headers, method=method), timeout=10) as response:
        return response.status, json.load(response)


def main():
    values = env_values()
    _, customer = request("POST", "/auth-api/api/v1/auth/login", {
        "email": "jan@example.com", "password": values["DEMO_CUSTOMER_PASSWORD"]})
    _, admin = request("POST", "/auth-api/api/v1/auth/login", {
        "email": "admin@example.com", "password": values["DEMO_ADMIN_PASSWORD"]})
    customer_token = customer["token"]
    admin_token = admin["token"]

    _, offers = request("GET", "/offer-api/api/v1/offers")
    assert len([item for item in offers if item["name"] == "City Discovery Tour"]) == 1
    offer = next(item for item in offers if item["id"] == 900001)
    slot_path = f"/availability-api/api/v1/offers/{offer['id']}/availability"
    _, slots = request("GET", slot_path)
    assert len([item for item in slots if item["id"] == 900001]) == 1
    before = next(item for item in slots if item["id"] == 900001)

    status, reservation = request("POST", "/booking-api/api/v1/reservations", {
        "availabilitySlotId": before["id"],
        "customerName": "Jan Kowalski",
        "customerEmail": "jan@example.com",
        "partySize": 1,
    }, customer_token)
    assert status == 201 and reservation["status"] == "PENDING"
    reservation_id = reservation["id"]

    _, confirmed = request("POST", f"/booking-api/api/v1/admin/reservations/{reservation_id}/confirm",
                           token=admin_token)
    assert confirmed["status"] == "CONFIRMED"
    _, cancelled = request("DELETE", f"/booking-api/api/v1/reservations/{reservation_id}",
                           token=customer_token)
    assert cancelled["status"] == "CANCELLED"

    _, slots_after = request("GET", slot_path)
    after = next(item for item in slots_after if item["id"] == before["id"])
    assert after["reservedCount"] == before["reservedCount"]
    print("Real API smoke passed: login, offer, slot, booking, admin confirm, cancel, capacity restored")


if __name__ == "__main__":
    try:
        main()
    except HTTPError as error:
        print(f"HTTP {error.code} at {error.url}")
        raise
