# API Contract

Base path: `/api/v1`.

Protected endpoints use JWT bearer authentication. Get a token from the auth
module and send it as:

```text
Authorization: Bearer <jwt>
```

Rules:

```text
Public GET /offers and /offers/{offerId}/availability: no authentication.
Admin endpoints under /admin/**: ADMIN role.
Reservation endpoints under /reservations/**: CUSTOMER or ADMIN role.
CUSTOMER users can access only reservations matching the email in their token.
ADMIN users can access all reservations.
```

The auth module exposes customer registration, login and password change
endpoints. Login returns a JWT bearer token. In the `dev` and `dev-postgres`
profiles, the auth module seeds an admin account by default:

```text
admin@example.com / admin123
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
AUTHENTICATION_FAILED
USER_ACCOUNT_ALREADY_EXISTS
INVALID_BEARER_TOKEN
ACCESS_DENIED
OFFER_NOT_FOUND
AVAILABILITY_SLOT_NOT_FOUND
RESERVATION_NOT_FOUND
RESERVATION_ACCESS_DENIED
```

## Auth API

Register customer:

```bash
curl -X POST http://localhost:8083/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "displayName": "Jan Kowalski",
    "email": "jan@example.com",
    "password": "customer123"
  }'
```

Login:

```bash
curl -X POST http://localhost:8083/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "jan@example.com",
    "password": "customer123"
  }'
```

Successful login response:

```json
{
  "token": "<jwt>",
  "tokenType": "Bearer",
  "expiresInSeconds": 7200
}
```

Change password:

```bash
curl -X POST http://localhost:8083/api/v1/auth/change-password \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "currentPassword": "customer123",
    "newPassword": "customer456"
  }'
```

Successful password change returns `204 No Content`.

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
curl -H "Authorization: Bearer $ADMIN_TOKEN" http://localhost:8080/api/v1/admin/offers
```

Admin create offer:

```bash
curl -X POST http://localhost:8080/api/v1/admin/offers \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
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
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "price": 219.99
  }'
```

Admin archive:

```bash
curl -H "Authorization: Bearer $ADMIN_TOKEN" -X DELETE http://localhost:8080/api/v1/admin/offers/1
```

## Availability API

Public list open slots for an offer:

```bash
curl http://localhost:8081/api/v1/offers/1/availability
```

Admin create slot:

```bash
curl -X POST http://localhost:8081/api/v1/admin/offers/1/availability \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
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
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "capacity": 12
  }'
```

Admin cancel slot:

```bash
curl -H "Authorization: Bearer $ADMIN_TOKEN" -X DELETE http://localhost:8081/api/v1/admin/availability/1
```

## Availability Business Rules

- An offer cannot have multiple availability slots with the same start and end
  time. Use `capacity` to represent multiple seats.
- Duplicate availability slot creation or update returns `409 Conflict` with
  `AVAILABILITY_SLOT_ALREADY_EXISTS`.

## Booking API

Create reservation:

```bash
curl -X POST http://localhost:8082/api/v1/reservations \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
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
curl -H "Authorization: Bearer $CUSTOMER_TOKEN" http://localhost:8082/api/v1/reservations/1
```

Find reservations by customer email:

```bash
curl -H "Authorization: Bearer $CUSTOMER_TOKEN" "http://localhost:8082/api/v1/reservations?customerEmail=jan@example.com"
```

Admin list all reservations:

```bash
curl -H "Authorization: Bearer $ADMIN_TOKEN" http://localhost:8082/api/v1/admin/reservations
```

Admin list reservations for a slot:

```bash
curl -H "Authorization: Bearer $ADMIN_TOKEN" http://localhost:8082/api/v1/admin/availability/1/reservations
```

Admin confirm pending reservation:

```bash
curl -H "Authorization: Bearer $ADMIN_TOKEN" -X POST http://localhost:8082/api/v1/admin/reservations/1/confirm
```

Admin reject pending reservation:

```bash
curl -H "Authorization: Bearer $ADMIN_TOKEN" -X POST http://localhost:8082/api/v1/admin/reservations/1/reject
```

Cancel reservation:

```bash
curl -H "Authorization: Bearer $CUSTOMER_TOKEN" -X DELETE http://localhost:8082/api/v1/reservations/1
```

## Booking Business Rules

- Creating a reservation requires an existing availability slot.
- Creating a reservation creates a `PENDING` reservation and increases
  `reservedCount` on the slot.
- Admin confirmation changes a pending reservation to `CONFIRMED` without
  changing capacity, because pending reservations already hold capacity.
- Admin rejection changes a pending reservation to `REJECTED` and releases
  reserved capacity.
- If `partySize` would exceed remaining capacity, the API returns
  `BUSINESS_RULE_VIOLATION`.
- Canceling a reservation changes its status to `CANCELLED`.
- Canceling a pending or confirmed reservation releases reserved capacity.
- Confirming or rejecting a non-pending reservation returns
  `BUSINESS_RULE_VIOLATION`.
- Canceling a reservation releases its `partySize` from the slot.
- Canceling an already-cancelled reservation returns `BUSINESS_RULE_VIOLATION`.
