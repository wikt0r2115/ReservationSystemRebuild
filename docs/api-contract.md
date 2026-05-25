# API Contract

Base path: `/api/v1`.

The current MVP uses HTTP Basic authentication:

```text
admin:    admin / admin123
customer: customer / customer123
```

Rules:

```text
Public GET /offers and /offers/{offerId}/availability: no authentication.
Admin endpoints under /admin/**: ADMIN role.
Reservation endpoints under /reservations/**: CUSTOMER or ADMIN role.
```

## Error Shape

All module-local exception handlers return the same shape:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Validation failed",
  "details": [
    {
      "field": "customerEmail",
      "message": "must be a well-formed email address"
    }
  ]
}
```

Common codes:

```text
VALIDATION_ERROR
INVALID_REQUEST_BODY
BUSINESS_RULE_VIOLATION
OFFER_NOT_FOUND
AVAILABILITY_SLOT_NOT_FOUND
RESERVATION_NOT_FOUND
```

## Offer API

Public list active offers:

```bash
curl http://localhost:8080/api/v1/offers
```

Public get active offer:

```bash
curl http://localhost:8080/api/v1/offers/1
```

Admin list all offers:

```bash
curl -u admin:admin123 http://localhost:8080/api/v1/admin/offers
```

Admin create offer:

```bash
curl -X POST http://localhost:8080/api/v1/admin/offers \
  -u admin:admin123 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "City Tour",
    "imageUrl": "https://example.com/city.jpg",
    "description": "Full day city tour",
    "price": 199.99
  }'
```

Admin partial update:

```bash
curl -X PATCH http://localhost:8080/api/v1/admin/offers/1 \
  -u admin:admin123 \
  -H "Content-Type: application/json" \
  -d '{
    "price": 219.99
  }'
```

Admin archive:

```bash
curl -u admin:admin123 -X DELETE http://localhost:8080/api/v1/admin/offers/1
```

## Availability API

Public list open slots for an offer:

```bash
curl http://localhost:8081/api/v1/offers/1/availability
```

Admin create slot:

```bash
curl -X POST http://localhost:8081/api/v1/admin/offers/1/availability \
  -u admin:admin123 \
  -H "Content-Type: application/json" \
  -d '{
    "startsAt": "2099-06-02T10:00:00",
    "endsAt": "2099-06-02T12:00:00",
    "capacity": 10
  }'
```

Admin partial update:

```bash
curl -X PATCH http://localhost:8081/api/v1/admin/availability/1 \
  -u admin:admin123 \
  -H "Content-Type: application/json" \
  -d '{
    "capacity": 12
  }'
```

Admin cancel slot:

```bash
curl -u admin:admin123 -X DELETE http://localhost:8081/api/v1/admin/availability/1
```

## Booking API

Create reservation:

```bash
curl -X POST http://localhost:8082/api/v1/reservations \
  -u customer:customer123 \
  -H "Content-Type: application/json" \
  -d '{
    "availabilitySlotId": 1,
    "customerName": "Jan Kowalski",
    "customerEmail": "jan@example.com",
    "partySize": 2
  }'
```

Get reservation by id:

```bash
curl -u customer:customer123 http://localhost:8082/api/v1/reservations/1
```

Find reservations by customer email:

```bash
curl -u customer:customer123 "http://localhost:8082/api/v1/reservations?customerEmail=jan@example.com"
```

Admin list all reservations:

```bash
curl -u admin:admin123 http://localhost:8082/api/v1/admin/reservations
```

Admin list reservations for a slot:

```bash
curl -u admin:admin123 http://localhost:8082/api/v1/admin/availability/1/reservations
```

Cancel reservation:

```bash
curl -u customer:customer123 -X DELETE http://localhost:8082/api/v1/reservations/1
```

## Booking Business Rules

- Creating a reservation requires an existing availability slot.
- Creating a reservation increases `reservedCount` on the slot.
- If `partySize` would exceed remaining capacity, the API returns
  `BUSINESS_RULE_VIOLATION`.
- Canceling a reservation changes its status to `CANCELLED`.
- Canceling a reservation releases its `partySize` from the slot.
- Canceling an already-cancelled reservation returns `BUSINESS_RULE_VIOLATION`.
