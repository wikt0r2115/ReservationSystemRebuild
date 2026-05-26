# Offer Module V1 Plan

## Goal

Deliver the first complete module of the reservation backend: offer management.

V1 should be small, coherent and portfolio-readable. The module should show
clean REST API design, DTO mapping, validation, business rules, persistence,
tests and documentation without introducing availability or reservations yet.

## Current Baseline

Current module path:

```text
reservation/offer
```

Already present:

- `Offer` JPA entity;
- `OfferRepository`;
- `OfferService`;
- `OfferController`;
- create/list/archive endpoints;
- basic validation;
- global exception handler;
- domain, service and controller tests;
- Maven Wrapper;
- Spring Boot 4.0.6.

## Target Scope For V1

V1 covers only offers.

Included:

- create offer;
- list active offers;
- list all offers for admin;
- get offer by id;
- update offer;
- archive offer;
- consistent API prefix;
- consistent DTOs;
- consistent errors;
- tests for domain, service, repository and API behavior;
- README examples for all offer endpoints.

Excluded from V1:

- availability slots;
- reservations;
- authentication;
- PostgreSQL/Flyway migration rollout;
- Docker Compose;
- payments, email, calendar integrations.

These exclusions are intentional. They keep the first module focused and make
the next milestone easier to review.

## Proposed API

Use a versioned API prefix:

```text
/api/v1
```

Customer/public endpoints:

```text
GET /api/v1/offers
GET /api/v1/offers/{offerId}
```

Admin endpoints:

```text
GET    /api/v1/admin/offers
POST   /api/v1/admin/offers
PATCH  /api/v1/admin/offers/{offerId}
DELETE /api/v1/admin/offers/{offerId}
```

For V1, admin endpoints do not need authentication yet. Document this clearly
as a V1 limitation.

## DTOs

Create request:

```java
CreateOfferRequest(
    String name,
    String imageUrl,
    String description,
    BigDecimal price
)
```

Update request:

```java
UpdateOfferRequest(
    String name,
    String imageUrl,
    String description,
    BigDecimal price
)
```

At least one field should be required for update. Null means "leave unchanged".

Response:

```java
OfferResponse(
    Long id,
    String name,
    String imageUrl,
    String description,
    BigDecimal price,
    boolean archived
)
```

Keep response DTOs separate from entities. Controllers should never return JPA
entities directly.

## Domain Rules

Offer creation:

- name is required, trimmed and max 255 chars;
- image URL is required, trimmed and max 2048 chars;
- description is required, trimmed and max 2048 chars;
- price is required, positive, max `99999.99` and max two decimal places;
- new offer starts as active.

Offer update:

- archived offers cannot be changed;
- unchanged values should either be accepted as no-op or return a clear
  business error. Choose one behavior and test it;
- price updates must follow the same price rules as creation;
- text fields must be trimmed.

Offer archive:

- archived offers disappear from public list;
- archive should be idempotent only if explicitly chosen. Current behavior
  returns a business error on second archive, which is acceptable if documented
  and tested.

Public reads:

- public list returns only non-archived offers;
- public get by id should return `404` for missing or archived offer.

Admin reads:

- admin list returns active and archived offers;
- admin get by id can return archived offers.

## Error Model

Replace the current minimal error with a more expressive shape:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "details": [
    {
      "field": "price",
      "message": "must be greater than 0"
    }
  ]
}
```

Recommended error codes:

```text
VALIDATION_ERROR
OFFER_NOT_FOUND
BUSINESS_RULE_VIOLATION
INTERNAL_ERROR
```

HTTP mapping:

- validation errors: `400`;
- business rule violations: `400` or `409`, choose consistently;
- missing offer: `404`;
- unexpected errors: `500`.

## Implementation Steps

### Step 1: API Prefix And DTO Naming

- Move endpoints from `/offers` to `/api/v1`.
- Introduce `OfferResponse`.
- Rename `OfferDTO` to `OfferResponse` or replace it cleanly.
- Keep `CreateOfferRequest`.
- Add `UpdateOfferRequest`.
- Add mapper methods in controller or a small package-private mapper class.

Done when:

- old controller tests are updated to `/api/v1/...`;
- controller does not expose `Offer` directly;
- response includes `archived`.

### Step 2: Complete Service API

Add service methods:

```java
List<Offer> findActiveOffers();
List<Offer> findAllOffers();
Offer findActiveOfferById(Long offerId);
Offer findOfferById(Long offerId);
Offer createOffer(...);
Offer updateOffer(Long offerId, UpdateOfferCommand command);
Offer archiveOffer(Long offerId);
```

Use either request DTOs directly in service or create a service command object.
For cleaner layering, prefer command objects if the update logic becomes noisy.

Done when:

- service owns all transaction boundaries;
- controller contains no business decisions;
- missing offer behavior is tested.

### Step 3: Complete Repository Queries

Add repository methods:

```java
List<Offer> findByArchivedFalse();
Optional<Offer> findByIdAndArchivedFalse(Long id);
```

Optionally add ordering:

```java
List<Offer> findByArchivedFalseOrderByNameAsc();
List<Offer> findAllByOrderByNameAsc();
```

Done when:

- public reads cannot accidentally return archived offers;
- repository behavior is covered by a focused JPA test.

### Step 4: Domain Cleanup

- Implement URL validation in domain or rely only on request validation and
  document the boundary.
- Remove duplicated validation if it becomes inconsistent.
- Decide whether same-value updates are no-op or business errors.
- Normalize price scale to two decimal places if accepted by the rules.

Recommended V1 decision:

- request DTO validates obvious API input;
- domain protects core invariants;
- same-value update is a no-op, not an error, unless there is a strong reason
  to reject it.

Done when:

- domain tests document the chosen behavior;
- no TODO comments remain in `Offer`.

### Step 5: Better Error Handling

- Replace `ApiErrorResponse` with `ApiErrorResponse(code, message, details)`.
- Add `ApiFieldError`.
- Update `GlobalExceptionHandler` to return all field validation errors.
- Add typed business exceptions if `IllegalArgumentException` becomes too
  vague.

Done when:

- invalid request API test checks field details;
- not-found API test checks stable error code;
- business rule API test checks stable error code.

### Step 6: API Tests

Cover:

- public list returns only active offers;
- public get active offer returns `200`;
- public get archived/missing offer returns `404`;
- admin list returns active and archived offers;
- admin create valid offer returns `201`;
- admin create invalid request returns `400`;
- admin update one field returns updated response;
- admin update invalid price returns `400`;
- admin archive returns `200` or `204`, choose one;
- admin archive missing offer returns `404`;
- admin archive already archived offer returns chosen business error.

Done when:

- controller tests cover all endpoints;
- JSON assertions check meaningful fields, not only status codes.

### Step 7: Service Tests

Cover:

- create saves valid offer;
- list active delegates to active repository query;
- list all delegates to all repository query;
- get active offer rejects archived/missing offer;
- update applies partial changes;
- update rejects archived offer;
- archive changes state and saves;
- archive missing offer throws `OfferNotFoundException`.

Done when:

- service tests cover business paths without starting Spring context.

### Step 8: Repository Test

Add a JPA test for:

- saving offer;
- finding active offers;
- excluding archived offers;
- finding active by id.

For V1, H2 is acceptable. In a later milestone this should move to PostgreSQL
Testcontainers.

Done when:

- repository query assumptions are verified by a persistence test.

### Step 9: README Update

README should include:

- current stack;
- module path;
- how to run tests;
- how to run app;
- all V1 endpoint examples with `curl`;
- clear note that admin endpoints are not secured in V1;
- next milestone: availability slots.

Done when:

- a reviewer can run and test the module from README alone.

### Step 10: Final V1 Verification

Run:

```bash
cd reservation/offer
./mvnw test
./mvnw clean package
```

Expected:

- all tests pass;
- no JUnit discovery warnings;
- no `open-in-view` warning;
- build success.

Mockito dynamic agent warning can be accepted in V1 if documented, or handled
with Maven Surefire configuration if time allows.

## Definition Of Done For Offer V1

The first module is V1-ready when:

- all offer endpoints use `/api/v1`;
- active/admin read behavior is intentionally separated;
- update endpoint supports partial update;
- archived offers cannot be modified;
- errors have stable codes and useful validation details;
- domain, service, repository and API tests pass;
- README documents all current endpoints;
- `./mvnw test` and `./mvnw clean package` pass;
- next milestone is clearly availability slots, not more offer scope creep.

## Recommended Commit Split

If keeping history clean:

1. `Upgrade Spring Boot and document offer V1 plan`
2. `Complete offer API V1`
3. `Add offer repository and API test coverage`
4. `Polish offer README examples`
