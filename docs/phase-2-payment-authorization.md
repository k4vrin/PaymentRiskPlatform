# Phase 2: Payment Authorization Workflow

## Purpose

Phase 2 builds the first real business workflow in the platform: payment authorization.

In plain terms, this phase answers the question:

```text
When a merchant asks us to authorize a payment, how do we safely validate it, avoid duplicate work, evaluate risk,
store the result, publish events, and return a stable API response?
```

This phase is larger than just adding a REST endpoint. A payment authorization system must be safe under retries,
traceable through logs and events, durable in the database, and clear enough that later lookup, reversal, audit,
settlement, and operations features can build on it.

## Product Behavior We Are Building

The main API is:

```http
POST /api/v1/payments/authorize
```

A merchant sends a request like:

```json
{
  "merchantId": "mer_01HX7Q9K2V6M8P4A3B9C1D2E3F",
  "customerId": "cus_01HX7QAF4CQ8YFZ3M9N2W1P0VK",
  "amountMinor": 1299,
  "currency": "USD",
  "paymentMethodToken": "pmt_tok_4f7b8d9c2a1e",
  "deviceFingerprint": "dfp_6d9f1a2b3c4e5f678901",
  "externalReference": "order_2026_000123",
  "idempotencyKey": "idem_01HX7QK9JP7E5W5NRZ6T5Q3R1A"
}
```

The service must:

1. Validate the request.
2. Resolve or generate a correlation ID.
3. Convert the API request into an application command.
4. Check idempotency before creating new payment work.
5. Create a payment aggregate.
6. Ask the risk service for a decision.
7. Transition the payment to `AUTHORIZED` or `DECLINED`.
8. Persist payment state.
9. Persist the idempotency response snapshot.
10. Create outbox events.
11. Return a stable response.

The final response should look like:

```json
{
  "paymentId": "pay_01HX7R0BYV9Y6CNW3HZ7R8E4P2",
  "status": "AUTHORIZED",
  "authorizationCode": "AUTH-ABCDEFG123",
  "riskDecision": "APPROVED",
  "reasonCodes": ["LOW_RISK", "KNOWN_DEVICE"],
  "correlationId": "corr_01HX7R2HBK51S6ZGJ7FN9K4M8D",
  "riskScore": 18,
  "ruleVersion": "risk-rules-v1",
  "createdAt": "2026-05-25T10:15:30Z"
}
```

## Why Phase 2 Is Not Just One Service Method

A beginner mistake would be to put all behavior directly inside the controller or a single service class. That would
work for a demo, but it would become hard to test, replace, or operate.

This project uses boundaries:

- API layer: receives HTTP, validates DTOs, maps request/response shapes.
- Application layer: coordinates the workflow.
- Domain layer: owns payment rules and state transitions.
- Infrastructure layer: talks to database, Redis, gRPC, Kafka, and RabbitMQ.

The main idea is that business workflow code should depend on interfaces, not concrete infrastructure. For example,
the authorization service should know that it can ask an idempotency store for a result. It should not care whether
that result came from memory, Redis, PostgreSQL, or a future Oracle adapter.

## Important Terms

### Payment

The main business aggregate. It represents one payment authorization attempt and its lifecycle state.

Examples of lifecycle states:

- `RECEIVED`
- `RISK_PENDING`
- `AUTHORIZED`
- `DECLINED`
- `REVERSED`
- `FAILED`

### Payment Authorization

The authorization-specific state inside a payment. It records whether authorization was requested, risk is pending,
the payment was authorized, the payment was declined, or the attempt failed.

### Risk Decision

The result returned by the risk service. It includes:

- decision: approved, declined, or review-required;
- score;
- reason codes;
- rule hits or rule summary;
- rule version.

### Idempotency

Idempotency means a client can safely retry the same request without creating duplicate business work.

If the merchant sends the same authorization request twice with the same idempotency key, the second call should return
the original response. It must not create a second payment.

If the merchant reuses the same idempotency key for a different request, the service must reject it with
`IDEMPOTENCY_KEY_CONFLICT`.

### Request Fingerprint

A fingerprint is a stable hash of the request fields that matter for idempotency. It lets the service answer:

```text
Is this retry really the same request, or did the client reuse the same key for different data?
```

For authorization, the fingerprint should include fields such as merchant, customer, amount, currency, token,
device fingerprint, and external reference.

### Outbox Event

An outbox event is an event row written to the relational database in the same business workflow as payment state. A
separate publisher later reads the row and publishes it to Kafka.

This avoids the classic bug where the database update succeeds but Kafka publishing fails, leaving the system without
a durable event.

## Target End-To-End Flow

The complete Phase 2 authorization flow should be:

```text
Client
  -> POST /api/v1/payments/authorize
  -> CorrelationIdWebFilter resolves X-Correlation-Id
  -> PaymentAuthorizationController receives AuthorizationRequest
  -> Bean Validation validates request shape
  -> AuthorizationRequestMapper creates AuthorizePaymentCommand
  -> DefaultAuthorizePaymentService starts workflow
  -> IdempotencyResultStore checks existing result by scope + key
      -> if same fingerprint exists, return stored response
      -> if different fingerprint exists, throw IDEMPOTENCY_KEY_CONFLICT
      -> if missing, continue
  -> Payment aggregate is created
  -> Payment moves to RISK_PENDING
  -> RiskScoringClient calls Go gRPC risk service
  -> Risk decision maps to domain result
  -> Payment moves to AUTHORIZED or DECLINED
  -> Payment state is persisted
  -> Idempotency record is completed with response snapshot
  -> Outbox event is inserted
  -> Transaction commits
  -> AuthorizationResponse is returned
```

## Current State In The Codebase

The project currently has these Phase 2 foundations:

- payment API DTOs;
- payment controller;
- command mapper;
- payment domain model;
- payment value objects;
- payment lifecycle transitions;
- database migration for payments, authorizations, risk decisions, idempotency records, and outbox events;
- R2DBC row models and repositories;
- persistence mapper for payment rows;
- contract-only authorization service;
- in-memory idempotency result store behind an `IdempotencyResultStore` interface.

The current authorization service is still contract-only. It creates a payment aggregate in memory, applies a fake
approved risk decision, returns a response, and uses in-memory idempotency so duplicate calls in the same process return
the same response.

That is useful as a stepping stone, but it is not the final workflow.

## What Is Still Missing

The main missing pieces are:

- database-backed idempotency;
- Redis response snapshot cache;
- durable payment state persistence inside the authorization workflow;
- real Java gRPC risk client;
- risk decision mapping policy;
- outbox event creation;
- transaction boundary;
- full API/integration tests for the durable workflow.

## Layer-By-Layer Explanation

### API Layer

Files live under:

```text
services/payment-orchestrator-service/src/main/java/dev/kavrin/paymentrisk/payment/api
```

The API layer should do only HTTP-specific work:

- receive JSON;
- validate DTO annotations;
- read correlation ID from the exchange;
- map DTO to command;
- call the application service;
- map result to response DTO.

It should not:

- create payment rows;
- call repositories;
- call Redis;
- call gRPC;
- implement business decisions.

Keeping the controller thin makes it easier to test business rules without running WebFlux.

### Application Layer

Files live under:

```text
services/payment-orchestrator-service/src/main/java/dev/kavrin/paymentrisk/payment/application
services/payment-orchestrator-service/src/main/java/dev/kavrin/paymentrisk/idempotency/application
services/payment-orchestrator-service/src/main/java/dev/kavrin/paymentrisk/risk/application
```

The application layer coordinates a use case. For authorization, it should answer:

```text
What are the steps to authorize a payment?
```

It should depend on ports/interfaces such as:

- `AuthorizePaymentService`
- `IdempotencyResultStore`
- future payment persistence port;
- future risk scoring port;
- future outbox writer port.

It should not depend directly on concrete Redis clients, R2DBC repositories, gRPC stubs, or Kafka producers.

### Domain Layer

Files live under:

```text
services/payment-orchestrator-service/src/main/java/dev/kavrin/paymentrisk/payment/domain
```

The domain layer owns business concepts:

- `Payment`
- `PaymentAuthorization`
- `PaymentRiskDecision`
- `PaymentStatus`
- value objects such as `PaymentId`, `MerchantId`, `Money`, `AuthorizationCode`

The domain layer should make invalid states hard to represent. For example, the service should not randomly set a
string status to `"AUTHORIZED"`. It should call a domain method that enforces the transition.

### Infrastructure Layer

Files live under paths such as:

```text
payment/infrastructure/persistence
risk/infrastructure/grpc
idempotency/infrastructure/redis
outbox/infrastructure/persistence
```

Infrastructure code talks to external systems:

- PostgreSQL or Oracle-compatible database;
- Redis;
- gRPC risk service;
- Kafka;
- RabbitMQ.

Infrastructure adapters implement application interfaces. This lets the application workflow stay testable with fake
implementations.

## Idempotency Design

Idempotency has three stages in this project.

### Stage 1: In-Memory Store

This is the current implementation. It stores completed results in a `ConcurrentHashMap`.

It proves the behavior:

- same key + same fingerprint returns stored response;
- same key + different fingerprint throws conflict;
- expired entry can be replaced.

It does not survive process restart. It is not enough for production-like durability.

### Stage 2: Database Store

The database store is the next durable step.

It should use `idempotency_records` with fields like:

- `scope`
- `idempotency_key`
- `request_fingerprint`
- `payment_id`
- `status`
- `response_status`
- `response_body_json`
- `expires_at`
- `created_at`
- `updated_at`

The database is the source of truth. If the service restarts, it can still know whether a key was used.

### Stage 3: Redis Cache

Redis should be added after the database store.

Redis is an optimization:

- faster duplicate response lookup;
- TTL-based snapshot cache;
- lower database read pressure for retries.

Redis is not the source of truth. If Redis misses, the service must check the database.

## Payment Persistence Design

Payment state must be persisted because later features depend on it:

- payment lookup;
- reversal;
- audit;
- replay;
- operations investigation;
- settlement projections.

The authorization workflow must save:

- `PaymentRow`: the main payment state;
- `PaymentAuthorizationRow`: the current authorization state;
- `PaymentRiskDecisionRow`: the risk decision when available.

The service should use a payment persistence port before using concrete repositories. A junior developer should think
about it this way:

```text
Application service says: "save this payment state."
Infrastructure adapter says: "I know how to turn that into rows and repositories."
```

That separation keeps business code clean.

## Risk Service Design

The Java payment service should not implement the risk rules directly. It should call the Go risk service through gRPC.

The Java side needs a risk client port such as:

```text
RiskScoringClient.scorePayment(...)
```

The gRPC adapter then implements that port using generated protobuf classes.

The application service should not know about protobuf-generated classes. It should use internal request/response
records, and the adapter should translate between internal models and protobuf models.

## Outbox Design

When payment state changes, other systems need to know. For example:

- audit consumer needs a history;
- settlement projection needs authorized/reversed payments;
- ops metrics need counts and lag;
- replay tooling needs durable event records.

The authorization service should create outbox rows such as:

- `PaymentAuthorizationRequested`
- `PaymentAuthorized`
- `PaymentDeclined`

These rows are later published to Kafka by an outbox relay worker.

The important rule:

```text
Do not publish directly to Kafka inside the payment transaction.
Write an outbox row, commit it, then publish asynchronously.
```

## Transaction Boundary

The transaction boundary is one of the most important design choices.

The durable writes that must be consistent are:

- payment state;
- authorization state;
- risk decision state;
- idempotency completion snapshot;
- outbox event rows.

If one of those fails, the system should not partially commit a successful authorization without the idempotency result
or event record.

The tricky part is the remote risk call. We generally do not want to hold a database transaction open while waiting for
a remote gRPC service. A practical Phase 2 approach is:

1. Validate request.
2. Check idempotency.
3. Call risk service.
4. Open transaction.
5. Persist payment/risk/idempotency/outbox.
6. Commit transaction.

This may evolve later, but the selected order must be documented and tested.

## Error Behavior

All public errors should use `ApiErrorResponse`.

Important Phase 2 errors:

- invalid request: `400 INVALID_REQUEST` or validation failure;
- missing or malformed request body: `400 MALFORMED_REQUEST`;
- idempotency key conflict: `409 IDEMPOTENCY_KEY_CONFLICT`;
- risk timeout: `504 RISK_SERVICE_TIMEOUT`;
- risk service unavailable: `503 DOWNSTREAM_UNAVAILABLE`;
- unexpected failure: `500 INTERNAL_ERROR`.

Never expose sensitive request values in errors.

## Correlation ID Behavior

Every authorization request should have a correlation ID.

The correlation ID should appear in:

- HTTP response header;
- API response body;
- error response body;
- logs;
- risk gRPC request metadata or message field;
- outbox event envelope;
- Kafka headers when events are later published.

This is what lets an operator trace one payment through the whole system.

## Step-By-Step Implementation Plan

### Step 1: Idempotency Port

Status: complete.

We introduced `IdempotencyResultStore`, so the authorization service depends on an interface. The current implementation
is `InMemoryIdempotencyResultStore`.

Why this matters:

- a fake implementation can be used in tests;
- a database implementation can be added without rewriting the authorization workflow;
- Redis can be added later as another adapter or decorator.

### Step 2: Idempotency Record Mapper

Build a mapper that converts between application idempotency concepts and `IdempotencyRecordRow`.

This should be small and heavily tested because mapping bugs cause dangerous retry behavior.

### Step 3: JSON Response Snapshot Serialization

The idempotency record must store the original response.

For now, the response snapshot type is `AuthorizePaymentResult`. We need JSON serialization and deserialization so a
duplicate request can return the same response even after a restart.

### Step 4: Database Idempotency Read Path

Before doing new payment work, check the database:

- no row: continue;
- expired row: continue or replace according to chosen policy;
- same fingerprint: return stored response;
- different fingerprint: conflict.

### Step 5: Database Idempotency Write Path

When accepting a new key:

- insert `STARTED`;
- later update to `COMPLETED` with response JSON;
- handle failures carefully.

This is where unique constraints protect against races.

### Step 6: Wire Database Idempotency Into Authorization

Replace production use of the in-memory store with database-backed idempotency. Keep the in-memory store for unit tests
if it remains useful.

### Step 7: Sensitive Data Hashing

Before persistence, do not store raw payment tokens or raw device fingerprints.

Add deterministic hashing helpers so the same token/fingerprint can be matched or investigated without storing the raw
secret value.

### Step 8: Payment State Persistence Port

Add an application interface for saving payment state. The authorization service should depend on this port.

### Step 9: Durable Payment Write Adapter

Implement the persistence port with R2DBC repositories and `PaymentPersistenceMapper`.

It should save:

- `PaymentRow`;
- `PaymentAuthorizationRow`;
- `PaymentRiskDecisionRow`.

### Step 10: Wire Payment Persistence Into Authorization

After the payment reaches `AUTHORIZED` or `DECLINED`, persist it and return a response based on the persisted result.

### Step 11: Risk Client Port

Create an application interface for risk scoring. The payment service should call the interface, not gRPC directly.

### Step 12: Java gRPC Risk Adapter

Implement the risk client port using generated protobuf classes and gRPC timeout handling.

### Step 13: Risk Decision Mapping Policy

Convert risk responses into domain decisions. Make review-required and timeout behavior explicit.

### Step 14: Wire Risk Into Authorization

Replace the contract-only approval with the real risk client.

### Step 15: Outbox Payload Records

Define event payload records for authorization outcomes.

### Step 16: Outbox Mapper

Map domain events into the shared event envelope.

### Step 17: Persist Outbox Events

Save outbox rows during the durable authorization workflow.

### Step 18: Transaction Boundary

Wrap the durable writes so payment state, idempotency completion, and outbox rows commit consistently.

### Step 19: Redis Idempotency Cache

Add Redis for fast duplicate response lookup.

### Step 20: Redis Miss Fallback

On Redis miss, read from the database and repopulate Redis if a completed snapshot exists.

### Step 21: API Documentation

Document request/response examples, idempotency behavior, risk timeout behavior, and emitted events.

### Step 22: Repository And Integration Tests

Use real database-backed tests where possible. Verify migrations, constraints, inserts, reads, and transaction behavior.

### Step 23: Authorization API Tests

Prove the public REST behavior:

- success;
- validation failure;
- duplicate replay;
- idempotency conflict;
- risk timeout.

## Testing Expectations

Every step should have tests at the right level.

Use unit tests for:

- value objects;
- mappers;
- domain transitions;
- idempotency decisions;
- risk mapping.

Use integration tests for:

- Flyway migrations;
- repositories;
- Redis adapter behavior;
- gRPC risk adapter behavior;
- transaction rollback;
- full API behavior.

## Common Mistakes To Avoid

- Do not put repository calls in the controller.
- Do not call Redis directly from the controller.
- Do not let the authorization service depend directly on generated gRPC stubs.
- Do not store raw payment tokens.
- Do not treat Redis as the durable idempotency source.
- Do not publish directly to Kafka before the database transaction commits.
- Do not return a different response for an exact duplicate idempotent retry.
- Do not reuse the same idempotency key for a different request.
- Do not hold a database transaction open longer than necessary around remote calls.

## Completion Criteria

Phase 2 is complete when:

- `POST /api/v1/payments/authorize` creates durable payment state;
- exact duplicate requests return the original response;
- conflicting idempotency reuse returns `IDEMPOTENCY_KEY_CONFLICT`;
- the Go risk service decides authorization outcomes through gRPC;
- approved payments reach `AUTHORIZED`;
- declined payments reach `DECLINED`;
- idempotency response snapshots are stored durably;
- Redis caches completed idempotency snapshots and falls back to the database;
- outbox events are created for authorization outcomes;
- durable writes are transactionally consistent;
- API, unit, repository, and integration tests cover the main success and failure paths.

## Related Documents

- PRD: `docs/Project.md`
- API roadmap: `docs/ApiRoadmap.md`
- Error contract: `docs/api/error-contract.md`
- Correlation ID contract: `docs/api/correlation-id.md`
- REST conventions: `docs/api/rest-api-conventions.md`
- Risk gRPC contract: `docs/api/risk-grpc-contract.md`
- Event envelope: `docs/events/event-envelope.md`
