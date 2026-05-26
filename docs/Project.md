---
type: prd
product: Reactive Payment Risk Platform
stack: [java, spring-boot, webflux, reactor, spring-security, rest, grpc, protobuf, go, microservices, postgresql, oracle-compatible-sql, redis, kafka, rabbitmq, docker, kubernetes, testcontainers, micrometer, prometheus, grafana, cicd]
status: draft
created: 2026-05-23
updated: 2026-05-26
tags: [prd, fintech, payments, risk, reactive, microservices]
---

# Product Requirements Document: Reactive Payment Risk Platform

## 1. Summary

Reactive Payment Risk Platform is a payment authorization and risk evaluation system for merchants and operations
teams. It accepts payment authorization requests, evaluates fraud/risk signals, returns authorization outcomes,
publishes durable business events, supports replay and audit workflows, and exposes operational visibility for failed
payments, dead letters, callbacks, and system health.

The product is also intended to demonstrate production-grade backend engineering: Java Spring Boot WebFlux for the main
orchestrator, a small Go gRPC risk service, relational persistence, Redis idempotency/cache behavior, Kafka event
processing, RabbitMQ command dispatch, security, observability, and automated testing.

## 2. Problem Statement

Payment authorization systems must make fast decisions while preserving correctness, auditability, and recovery paths.
Retries must not double-authorize payments. Downstream failures must produce stable behavior. Events must not be lost
after successful business transactions. Operations teams need enough visibility to investigate failures, replay work,
and understand platform health.

This platform solves those needs in a compact reference implementation with realistic fintech constraints.

## 3. Goals

- Provide a stable REST API for payment authorization, lookup, and reversal.
- Ensure payment authorization retries are idempotent.
- Evaluate each authorization through a risk-scoring service.
- Persist payment, risk, idempotency, outbox, audit, replay, and dead-letter records.
- Publish durable payment events through Kafka using the transactional outbox pattern.
- Dispatch targeted partner callback work through RabbitMQ.
- Provide operations APIs for investigation, replay, dead-letter review, and lag/health visibility.
- Maintain stable API error contracts and correlation IDs across service boundaries.
- Demonstrate enterprise Java, reactive Spring, distributed systems, and operational engineering practices.

## 4. Non-Goals

- Do not implement real card network integration.
- Do not store raw PAN, CVV, or sensitive payment credentials.
- Do not implement a full merchant onboarding product.
- Do not implement real settlement file generation in the first release.
- Do not make Kafka part of the synchronous authorization response path.
- Do not treat Redis as the source of truth for idempotency; database records remain durable authority.

## 5. Target Users

- Merchant API client: submits authorization and reversal requests and receives stable outcomes.
- Operations analyst: investigates failed payments, dead letters, outbox failures, replay jobs, and callback failures.
- Auditor/compliance reviewer: reviews immutable payment and risk event history.
- Platform engineer: operates services, monitors dashboards, diagnoses latency and failures, and manages recovery.
- Developer/reviewer: evaluates architecture, code quality, tests, and production readiness.

## 6. Personas And Jobs To Be Done

### Merchant API Client

- Submit a payment authorization request with an idempotency key.
- Safely retry an authorization request after timeout or network failure.
- Reverse a previously authorized payment.
- Receive predictable success and error responses.

### Operations Analyst

- Search payments by status, merchant, customer, and time range.
- Inspect risk decisions, reason codes, and payment lifecycle events.
- Review dead-lettered events and failed callbacks.
- Trigger replay or manual retry where allowed.

### Platform Engineer

- Track API latency, downstream latency, Kafka lag, outbox lag, Redis hit/miss rate, and database latency.
- Diagnose risk-service timeouts, Redis failures, Kafka publish failures, and database pool exhaustion.
- Use logs, metrics, traces/correlation IDs, runbooks, and dashboards to resolve incidents.

## 7. Product Scope

### In Scope

- `payment-orchestrator-service`: Java Spring Boot WebFlux service for authorization, reversal, lookup, idempotency,
  persistence orchestration, event creation, security, and operations APIs.
- `risk-scoring-service`: Go gRPC service that returns score, decision, rule hits, reason codes, and rule version.
- `event-relay-worker`: outbox publisher that emits durable payment events to Kafka.
- `payment-audit-consumer`: Kafka consumer that stores audit/investigation history.
- `settlement-projection-consumer`: Kafka consumer that builds settlement-ready read models.
- `partner-callback-worker`: RabbitMQ command consumer for merchant webhook callbacks.
- `local-platform`: Docker Compose stack for PostgreSQL, Redis, Kafka, RabbitMQ, Prometheus, Grafana, and local
  services.

### Out Of Scope For Initial Release

- Full production Kubernetes operations.
- Real external merchant webhook contracts beyond a simple callback command.
- Full settlement accounting.
- Multi-region deployment.
- Real authentication provider integration; local/dev credentials and role simulation are acceptable initially.

## 8. Functional Requirements

### FR1: Payment Authorization

- The system must expose `POST /api/v1/payments/authorize`.
- The request must include merchant ID, customer ID, amount, currency, payment method token, device fingerprint,
  optional external reference, and idempotency key.
- The service must validate request shape and domain constraints before durable processing.
- The service must create a payment authorization aggregate.
- The service must evaluate risk before returning the final authorization outcome.
- Approved risk decisions must transition payment to `AUTHORIZED`.
- Declined risk decisions must transition payment to `DECLINED`.
- Timeout/unavailable risk decisions must follow an explicit Phase 2 policy and return a stable response or error.
- The response must include payment ID, payment status, authorization code when authorized, risk decision, reason codes,
  correlation ID, risk score, rule version, and created timestamp.

### FR1.1: Payment State Persistence

- The system must persist the payment aggregate after authorization state changes.
- The system must persist the current payment lifecycle status.
- The system must persist the authorization state for each payment.
- The system must persist the risk decision associated with the authorization outcome.
- Persisted payment state must be queryable by payment ID for lookup, audit, replay, and operations workflows.
- Payment state persistence, idempotency completion, and outbox event creation must be made consistent through the
  selected transaction boundary.

### FR2: Idempotency

- Authorization and reversal requests must require idempotency keys.
- Idempotency keys must be validated for length and allowed characters.
- The system must compute a stable request fingerprint for idempotent commands.
- A retry with the same scope, key, and fingerprint must return the original response.
- A retry with the same scope and key but different fingerprint must return `IDEMPOTENCY_KEY_CONFLICT`.
- Durable idempotency records must store scope, key, request fingerprint, status, response status, response snapshot,
  expiry timestamp, and related payment ID when available.
- Redis may cache completed response snapshots with TTL, but database records remain the source of truth.
- On Redis miss, the system must fall back to the database idempotency record.

### FR3: Risk Scoring

- The Java orchestrator must call the Go gRPC risk service through protobuf.
- The risk request must include payment ID, amount, currency, merchant ID, customer ID, device fingerprint, and
  correlation ID.
- The risk response must include score, decision, reason codes, rule hits, and rule version.
- The Java service must map approved, declined, and review-required responses into internal payment outcomes.
- gRPC deadline exceeded must map to `RISK_SERVICE_TIMEOUT`.
- gRPC unavailable must map to `DOWNSTREAM_UNAVAILABLE`.

### FR4: Payment Lookup And Reversal

- The system must expose `GET /api/v1/payments/{paymentId}`.
- The lookup response must include payment, authorization, risk, reversal, and audit summary fields.
- The system must expose `POST /api/v1/payments/{paymentId}/reverse`.
- Reversal must require an idempotency key.
- Only reversible payment states may transition to `REVERSED`.
- Duplicate reversal requests must be idempotent or conflict based on request identity.
- Reversal must create a durable event.

### FR5: Event Publishing

- The platform must use a transactional outbox table for durable event creation.
- Kafka publishing must happen after the business transaction commits.
- Events must use stable event IDs, schema versions, occurred timestamps, producer names, correlation IDs, aggregate
  type, aggregate ID, and payload.
- Consumers must be idempotent.
- Poison messages must route to dead-letter handling.
- Replay status, retry count, last error, and next retry time must be tracked.

### FR6: Partner Callback Commands

- The platform must use RabbitMQ for targeted partner callback work.
- After terminal payment states, the orchestrator may enqueue `CallPartnerWebhook`.
- The callback worker must acknowledge only after successful delivery or terminal failure recording.
- Transient failures must retry with backoff.
- Exhausted messages must route to a dead-letter queue.
- Operations APIs must expose callback status and retry controls.

### FR7: Operations APIs

- The system must expose operations endpoints for payment search, failed outbox rows, dead letters, replay, and consumer
  lag.
- Operations APIs must require appropriate roles.
- Replay operations must be auditable and idempotent.

### FR8: Security And Audit

- The system must use role-based access for `MERCHANT`, `OPS`, `AUDITOR`, `ADMIN`, and `SERVICE`.
- Sensitive payment, customer, and device fields must be masked or hashed in logs, audit records, and persistence where
  raw values are not required.
- API keys/secrets must not be stored in plaintext.
- Authentication failures, authorization requests, reversals, replay attempts, and admin lookups must be auditable.
- Error responses must not echo secrets, payment tokens, raw device identifiers, or stack traces.

## 9. Non-Functional Requirements

### Reliability

- The authorization path must not double-create payments for retried requests with the same idempotency key and
  fingerprint.
- Payment state, idempotency completion, and outbox records must be committed consistently.
- Kafka publishing failures must not lose events.
- Redis failures must not make durable idempotency impossible when the database is available.

### Performance

- The authorization API must use explicit timeout budgets for Redis, database, gRPC, and messaging calls.
- Blocking work must not run on WebFlux event-loop threads.
- Rate limiting must protect authorization endpoints by merchant/client.
- Hot operational reads may use Redis-backed views where stale-data rules are documented.

### Observability

- All API responses and downstream calls must carry or derive a correlation ID.
- Logs must be structured and include correlation IDs.
- Metrics must include API latency, authorization throughput, risk latency/timeouts, Kafka producer failures, consumer
  lag, outbox lag, Redis hit/miss rate, database latency, dead-letter count, and replay outcomes.
- Dashboards must expose core service health and operational backlog.

### Maintainability

- Controllers must stay thin; workflow logic belongs in application services and domain types.
- Integrations must depend on ports/interfaces with infrastructure adapters for gRPC, Redis, database, Kafka, and
  RabbitMQ.
- Domain concepts must use typed value objects instead of raw strings where practical.
- Migrations must remain Oracle-compatible while supporting PostgreSQL locally.

### Testability

- Unit tests must cover domain rules, state transitions, validation, idempotency, risk mapping, and error contracts.
- Integration tests must cover REST APIs, database persistence, Redis cache/fallback, Kafka outbox publishing, RabbitMQ
  callbacks, and Go gRPC calls.
- Concurrency tests must cover duplicate authorization and replay behavior.
- Failure tests must cover risk timeout, Redis unavailable, Kafka publish failure, RabbitMQ retry exhaustion, database
  uniqueness conflict, and dead-letter recovery.

## 10. Core Data Model

### Payment Lifecycle

- `RECEIVED`
- `RISK_PENDING`
- `RISK_APPROVED`
- `AUTHORIZED`
- `DECLINED`
- `REVERSED`
- `FAILED`

### Main Records

- `Payment`
- `PaymentAuthorization`
- `RiskDecision`
- `Merchant`
- `CustomerProfile`
- `IdempotencyRecord`
- `OutboxEvent`
- `AuditEvent`
- `ReplayJob`
- `DeadLetterRecord`

## 11. Public API Surface

### Payment APIs

- `POST /api/v1/payments/authorize`
- `GET /api/v1/payments/{paymentId}`
- `POST /api/v1/payments/{paymentId}/reverse`

### Operations APIs

- `GET /api/v1/ops/payments?status=&from=&to=`
- `GET /api/v1/ops/dead-letters`
- `GET /api/v1/ops/outbox?status=FAILED`
- `POST /api/v1/ops/replay/{eventId}`
- `GET /api/v1/ops/consumer-lag`

## 12. Event And Messaging Requirements

### Kafka Topics

- `payment.authorization.requested`
- `risk.score.completed`
- `payment.authorization.completed`
- `payment.reversal.completed`
- `platform.dead-letter.recorded`

### Kafka Event Types

- `PaymentAuthorizationRequested`
- `RiskScoreCompleted`
- `PaymentAuthorized`
- `PaymentDeclined`
- `PaymentReversed`
- `DeadLetterRecorded`

### RabbitMQ Queue

- Queue: `partner.callback.commands`
- Dead-letter queue: `partner.callback.commands.dlq`
- Command: `CallPartnerWebhook`

## 13. Milestones

### M1: API Contract Baseline

- REST API conventions documented.
- Error contract documented and tested.
- Correlation ID behavior documented and tested.
- Protobuf contract generated for Java and Go.
- OpenAPI endpoint available.

### M2: Payment Authorization Workflow

- Payment domain model and state transitions implemented.
- Authorization endpoint implemented.
- Durable payment persistence implemented.
- Database-backed idempotency implemented.
- Redis idempotency cache implemented.
- Go risk service integrated through gRPC.
- Outbox records created with payment state.
- Authorization API tests cover success, validation failure, duplicate replay, conflict, and risk timeout.

### M3: Lookup And Reversal

- Payment lookup endpoint implemented.
- Reversal endpoint implemented.
- Reversal idempotency implemented.
- Reversal events emitted.
- Reversal tests cover success, duplicate retry, conflict, and invalid state.

### M4: Event Relay And Consumers

- Outbox relay publishes Kafka events.
- Audit consumer builds payment history.
- Settlement projection consumer builds settlement-ready read model.
- Dead-letter handling and replay implemented.

### M5: Partner Callback Commands

- RabbitMQ callback command producer implemented.
- Callback worker implemented.
- Retry and DLQ behavior implemented.
- Operations visibility for callback state implemented.

### M6: Security, Observability, And Operations

- Role-based access implemented.
- Sensitive data masking/hashing implemented.
- Metrics and dashboards implemented.
- Linux/Docker runbook documented.
- CI covers Java tests, Go tests, protobuf generation, compose validation, linting, and image build.

## 14. Acceptance Criteria

- A merchant can submit a valid authorization request and receive a stable authorization response.
- Retrying the same authorization request with the same idempotency key returns the original response.
- Reusing the same idempotency key for a different authorization request returns a structured conflict error.
- Approved risk decisions produce `AUTHORIZED` payments.
- Declined risk decisions produce `DECLINED` payments.
- Risk timeout/unavailable cases produce the selected stable Phase 2 behavior.
- Payment state and outbox records are persisted consistently.
- Kafka events can be published, consumed, retried, dead-lettered, and replayed.
- Operations users can inspect payments, outbox failures, dead letters, replay jobs, callback failures, and consumer
  lag.
- Sensitive values are not exposed in logs, error responses, or audit records.
- Testcontainers-backed tests prove database, Redis, Kafka, RabbitMQ, and gRPC integration paths.

## 15. Success Metrics

- Duplicate authorization retries create zero additional payments.
- Outbox relay eventually publishes all pending events or marks terminal failures for operations review.
- API error responses conform to `ApiErrorResponse`.
- Correlation IDs appear in API responses, logs, events, and gRPC metadata.
- Integration tests cover all primary success, duplicate, conflict, timeout, and replay paths.
- Local platform can start with one documented command and expose service health, metrics, and dashboards.

## 16. Open Decisions

- Review-required risk policy: authorize, decline, or return manual-review response.
- Risk timeout policy: fail closed, fail open for selected merchants, or return downstream timeout.
- Initial database target for CI: PostgreSQL only or PostgreSQL plus Oracle-compatible validation.
- Redis cache TTL for idempotency snapshots.
- Whether `PaymentAuthorizationRequested` should be emitted before or after risk scoring in Phase 2.
- Authentication approach for local development versus production-like deployment.

## 17. Reference Documents

- API roadmap: `docs/ApiRoadmap.md`
- Phase 2 implementation guide: `docs/phase-2-payment-authorization.md`
- Error contract: `docs/api/error-contract.md`
- Correlation ID contract: `docs/api/correlation-id.md`
- REST conventions: `docs/api/rest-api-conventions.md`
- Risk gRPC contract: `docs/api/risk-grpc-contract.md`
- Event envelope: `docs/events/event-envelope.md`
