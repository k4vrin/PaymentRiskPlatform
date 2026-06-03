# Phase 3: Payment Lookup And Reversal Workflow

## Purpose

Phase 3 adds two capabilities on top of the durable authorization workflow:

```text
How can a merchant inspect a payment after authorization, and how can an authorized payment be explicitly reversed?
```

Lookup is the read side of the payment lifecycle. Reversal is the first corrective command after authorization. The
workflow must preserve the same design principles from Phase 2: thin WebFlux controllers, explicit domain rules,
database-backed idempotency for command retries, durable state changes, outbox events, and structured errors.

## Product Behavior We Are Building

Phase 3 adds two public APIs:

```http
GET /api/v1/payments/{paymentId}
POST /api/v1/payments/{paymentId}/reverse
```

Lookup returns a non-sensitive payment detail view:

```json
{
  "paymentId": "pay_01HX7R0BYV9Y6CNW3HZ7R8E4P2",
  "merchantId": "mer_01HX7Q9K2V6M8P4A3B9C1D2E3F",
  "customerId": "cus_01HX7QAF4CQ8YFZ3M9N2W1P0VK",
  "amountMinor": 1299,
  "currency": "USD",
  "status": "AUTHORIZED",
  "externalReference": "order_2026_000123",
  "authorization": {
    "status": "AUTHORIZED",
    "authorizationCode": "AUTH-ABCDEFG123",
    "authorizedAt": "2026-05-25T10:15:30Z"
  },
  "risk": {
    "decision": "APPROVED",
    "score": 18,
    "reasonCodes": ["LOW_RISK"],
    "ruleVersion": "risk-rules-v1",
    "decidedAt": "2026-05-25T10:15:30Z"
  },
  "reversal": null,
  "createdAt": "2026-05-25T10:15:30Z",
  "updatedAt": "2026-05-25T10:15:30Z"
}
```

Reversal accepts a small command body:

```json
{
  "idempotencyKey": "idem_rev_01HX7QK9JP7E5W5NRZ6T5Q3R1A",
  "reason": "merchant_request"
}
```

The successful reversal response should look like:

```json
{
  "paymentId": "pay_01HX7R0BYV9Y6CNW3HZ7R8E4P2",
  "reversalId": "rev_01HX7Z7YQK4J4M5S2G8T9V0W1X",
  "status": "REVERSED",
  "reason": "merchant_request",
  "correlationId": "corr_01HX7R2HBK51S6ZGJ7FN9K4M8D",
  "reversedAt": "2026-05-25T10:18:30Z"
}
```

## Important Rules

### Lookup

Lookup must not expose raw sensitive data. The response may include business identifiers, amount, currency, lifecycle
state, authorization status, risk summary, and reversal summary. It must not include raw payment method tokens, raw
device fingerprints, or hash material unless there is a specific operational endpoint for that later.

Missing payments should return the shared structured not-found error contract.

### Reversal

Only an authorized payment is reversible in Phase 3.

The selected behavior is:

- `AUTHORIZED` can move to `REVERSED`.
- `DECLINED`, `FAILED`, `RECEIVED`, and `RISK_PENDING` cannot be reversed.
- A duplicate reversal command with the same idempotency key and same fingerprint returns the original reversal
  response.
- Reusing the same idempotency key for a different reversal command returns `IDEMPOTENCY_KEY_CONFLICT`.
- Reversing an already reversed payment without a matching idempotency record returns a structured state conflict.

## Target Lookup Flow

```text
Client
  -> GET /api/v1/payments/{paymentId}
  -> CorrelationIdWebFilter resolves X-Correlation-Id
  -> PaymentLookupController validates paymentId
  -> PaymentLookupService loads payment details
  -> Read adapter loads payment, authorization, risk, and reversal rows
      -> if payment is missing, throw ResourceNotFoundException
  -> Controller maps result to PaymentDetailsResponse
  -> Response returns non-sensitive payment details
```

## Target Reversal Flow

```text
Client
  -> POST /api/v1/payments/{paymentId}/reverse
  -> CorrelationIdWebFilter resolves X-Correlation-Id
  -> PaymentReversalController validates ReversePaymentRequest
  -> Controller maps request to ReversePaymentCommand
  -> PaymentReversalService computes reversal idempotency fingerprint
  -> Idempotency store checks existing result by reversal scope + key
      -> if same fingerprint exists, return stored response
      -> if different fingerprint exists, throw IDEMPOTENCY_KEY_CONFLICT
      -> if missing, continue
  -> Insert STARTED idempotency record
  -> Load current payment state
  -> Validate payment is reversible
  -> Open transaction
  -> Update payment status to REVERSED
  -> Insert payment reversal row
  -> Insert reversal outbox event
  -> Complete idempotency record with response snapshot
  -> Transaction commits
  -> PaymentReversalResponse is returned
```

## Layer-By-Layer Design

### API Layer

Files should live under:

```text
services/payment-orchestrator-service/src/main/java/dev/kavrin/paymentrisk/payment/api
```

The API layer should:

- receive HTTP requests;
- validate DTOs and path parameters;
- read the correlation ID from the exchange;
- map DTOs to query/command models;
- call application services;
- map application results to response DTOs.

The API layer should not query repositories, update payment state, check idempotency records directly, or create outbox
events.

### Application Layer

Files should live under:

```text
services/payment-orchestrator-service/src/main/java/dev/kavrin/paymentrisk/payment/application
```

Lookup should be represented as a query use case. Reversal should be represented as a command use case. The application
layer owns workflow ordering and depends on ports for persistence/idempotency/outbox behavior.

Expected boundaries:

- `PaymentLookupService`
- `PaymentLookupPort` or a query adapter interface
- `ReversePaymentCommand`
- `PaymentReversalService`
- `PaymentStatePersistencePort` for durable payment state and reversal persistence
- existing `DatabaseIdempotencyResultOperations`
- existing `PaymentOutboxEventWriter` or a broadened outbox writer abstraction

### Domain Layer

Files should live under:

```text
services/payment-orchestrator-service/src/main/java/dev/kavrin/paymentrisk/payment/domain
```

The domain layer should own whether a payment can be reversed. The service should not decide by comparing status
strings. It should call a domain method or policy that enforces the allowed transition.

Expected additions:

- reversal ID/value object;
- reversal reason/value object;
- reversal state model if needed;
- payment transition from `AUTHORIZED` to `REVERSED`;
- domain tests for valid and invalid reversal states.

### Infrastructure Layer

Files should live under:

```text
services/payment-orchestrator-service/src/main/java/dev/kavrin/paymentrisk/payment/infrastructure
```

Infrastructure should:

- load payment detail rows for lookup;
- persist reversal rows;
- update payment status;
- insert outbox events;
- keep sensitive values out of API read models;
- use R2DBC insert/update operations deliberately for application-assigned IDs.

## Idempotency Design

Reversal must use its own scope, for example:

```text
payment_reversal
```

The reversal fingerprint should include the fields that define the command identity:

- payment ID;
- reversal reason;
- any future reversal amount or metadata fields if added.

The same idempotency key can safely be reused only for the same reversal command. It must not be accepted for a
different payment ID or different reversal reason.

Redis can be used for replaying completed reversal responses, but the database remains the source of truth. If Redis is
added for reversal snapshots, cache TTL must not outlive the durable idempotency record.

## Persistence Design

Phase 3 should add a `payment_reversals` table.

Suggested fields:

- `payment_reversal_id`
- `payment_id`
- `idempotency_key`
- `reason`
- `status`
- `requested_at`
- `reversed_at`
- `created_at`
- `updated_at`

The payment row should also reflect the lifecycle transition to `REVERSED`. The reversal insert, payment update,
idempotency completion, and outbox insert must be transactionally consistent.

## Outbox Design

Phase 3 should create durable outbox events for reversal outcomes.

Phase 3 emits only the final outcome event:

- `PaymentReversed`

`PaymentReversalRequested` is intentionally deferred. The reversal service writes `PaymentReversed` only after the domain
transition succeeds, and it persists the payment update, reversal row, idempotency completion, and outbox event in one
transaction. Events follow the same envelope conventions used by authorization events and are created with `PENDING`
status.

## Audit Placeholder Behavior

Phase 3 does not write separate audit rows. Lookup and reversal audit visibility is represented as documented event
shapes for later consumers:

- lookup audit shape: `PaymentLookupObserved`, with payment ID, merchant/customer context when available, correlation ID,
  request time, and lookup result status;
- reversal audit shape: `PaymentReversed`, with payment ID, reversal ID, merchant/customer context, reason, correlation
  ID, and reversed-at timestamp.

The implemented durable signal is the `PaymentReversed` outbox event. A later audit consumer can project that event into
audit storage without coupling the synchronous reversal path to an audit table.

## Error Behavior

All public errors should use `ApiErrorResponse`.

Important Phase 3 errors:

- invalid request: `400 VALIDATION_FAILED` or `400 INVALID_REQUEST`;
- malformed request body: `400 MALFORMED_REQUEST`;
- missing payment: `404 RESOURCE_NOT_FOUND` or `404 PAYMENT_NOT_FOUND`;
- idempotency key conflict: `409 IDEMPOTENCY_KEY_CONFLICT`;
- non-reversible payment state: `409 PAYMENT_STATE_CONFLICT`;
- unexpected failure: `500 INTERNAL_ERROR`.

Never expose sensitive payment token or device fingerprint values in errors.

## Correlation ID Behavior

Every lookup and reversal request should have a correlation ID.

The correlation ID should appear in:

- HTTP response header;
- reversal response body;
- error response body;
- outbox event envelope for reversal events;
- logs and later audit events.

Lookup responses do not need a body-level correlation ID unless the response DTO explicitly includes one, but errors and
events still need it.

## Step-By-Step Implementation Plan

### Step 1: Package And Boundary Structure

Create the API, command, query, service, and persistence boundaries needed for lookup and reversal before adding
behavior.

### Step 2: Reversal Schema

Add `payment_reversals` with indexes and constraints. Keep the migration focused and add database-backed tests.

### Step 3: Reversal Domain Policy

Model the reversible states explicitly. The important rule is that only authorized payments can be reversed in Phase 3.

### Step 4: Payment Aggregate Reversal Transition

Extend the payment aggregate so reversal is a domain transition, not a repository-only status update.

### Step 5: Reversal Persistence Adapter

Map reversal state to entities and persist it with explicit insert/update operations.

### Step 6: Payment Lookup Read Model

Create a stable non-sensitive read model for payment details.

### Step 7: Payment Lookup API

Expose `GET /api/v1/payments/{paymentId}` with structured not-found behavior.

### Step 8: Reversal API Contract

Add request/response DTOs and validation for reversal commands.

### Step 9: Reversal Idempotency

Add a reversal idempotency scope and fingerprint. Duplicate same-fingerprint requests replay the stored response.
Conflicting reuse returns `IDEMPOTENCY_KEY_CONFLICT`.

### Step 10: Reversal Service

Implement the command workflow: idempotency check, payment load, domain validation, durable writes, idempotency
completion, and response mapping.

### Step 11: Reversal Transaction Boundary

Wrap payment update, reversal insert, idempotency completion, and outbox insert in one transaction.

### Step 12: Reversal Outbox Events

Add reversal payloads, mapping, serialization tests, and pending event persistence.

### Step 13: Reversal Controller

Expose `POST /api/v1/payments/{paymentId}/reverse` with validation, correlation ID propagation, and structured errors.

### Step 14: Audit Event Placeholders

Define how lookup and reversal activity will be visible to later audit consumers. Phase 3 writes no audit rows directly;
it documents lookup audit shape and emits `PaymentReversed` outbox events for reversal audit projection.

### Step 15: Documentation And Tests

Update API docs and add unit, API, repository, and integration tests for the main success and failure paths.

## Testing Expectations

Use unit tests for:

- reversal domain policy;
- payment aggregate transition rules;
- query/command mappers;
- response DTO mappers;
- reversal idempotency replay and conflict behavior;
- reversal outbox payload/envelope mapping;
- idempotency snapshot serialization for authorization and reversal responses.

Use API tests for:

- lookup success and not-found;
- reversal success, validation failure, duplicate replay, idempotency conflict, not-found, and invalid state conflict.

Use integration tests for:

- lookup after successful authorization;
- reversal durable commit after successful authorization;
- duplicate reversal without a second reversal row or outbox event;
- rollback when reversal outbox insertion fails.
- idempotency fingerprint behavior;
- outbox payload mapping.

Use API tests for:

- lookup success;
- lookup not found;
- reversal success;
- reversal validation failure;
- duplicate reversal replay;
- idempotency conflict;
- non-reversible payment state.

Use integration tests for:

- Flyway migration;
- payment detail read adapter;
- reversal persistence;
- duplicate reversal idempotency;
- transaction rollback when outbox insert fails;
- successful reversal committing payment, reversal, outbox, and idempotency rows together.

## Common Mistakes To Avoid

- Do not expose raw payment method tokens or raw device fingerprints in lookup responses.
- Do not put repository access in controllers.
- Do not reverse a payment by setting a status string directly.
- Do not allow declined or failed payments to reverse.
- Do not treat a second reversal without a matching idempotency record as success.
- Do not create reversal rows without updating the payment lifecycle state.
- Do not update payment state without writing the reversal outbox event in the same transaction.
- Do not let Redis reversal snapshots outlive the durable idempotency record.

## Completion Criteria

Phase 3 is complete when:

- `GET /api/v1/payments/{paymentId}` returns non-sensitive payment details;
- missing payment lookup returns a structured not-found error;
- `POST /api/v1/payments/{paymentId}/reverse` reverses an authorized payment;
- declined and failed payments return a structured conflict on reversal;
- duplicate reversal requests return the original response without creating a second reversal;
- conflicting reversal idempotency reuse returns `IDEMPOTENCY_KEY_CONFLICT`;
- reversal writes payment state, reversal row, idempotency completion, and outbox event transactionally;
- API, unit, repository, and integration tests cover the main success and failure paths.

## Related Documents

- PRD: `docs/Project.md`
- API roadmap: `docs/ApiRoadmap.md`
- Payment authorization workflow: `docs/phase-2-payment-authorization.md`
- Error contract: `docs/api/error-contract.md`
- Correlation ID contract: `docs/api/correlation-id.md`
- REST conventions: `docs/api/rest-api-conventions.md`
- Event envelope: `docs/events/event-envelope.md`
