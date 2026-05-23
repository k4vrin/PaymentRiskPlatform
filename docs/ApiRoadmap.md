# Reactive Payment Risk Platform API Roadmap

This roadmap breaks the API and service work into practical development phases. Each phase should leave the project in a working, reviewable state with clear acceptance criteria.

Use the checkboxes as the implementation tracker.

## Phase 0: Project Foundation

Goal: Verify the generated services, align runtime versions with the accepted ADR, and prepare the project conventions before domain implementation starts.

### Steps

- [x] Create Spring Boot API project in `services/payment-orchestrator-service`.
- [x] Create Go service folder in `services/risk-scoring-service`.
- [x] Create documentation folders:
    - [x] `docs`
    - [x] `docs/adr`
- [x] Create platform and scripts folders:
    - [x] `platform`
    - [x] `scripts`
- [x] Add initial stack ADR:
    - [x] `docs/adr/0001-lock-project-stack-and-dependencies.md`
- [x] Generate Spring Boot service with:
    - [x] Java `25`
    - [x] Spring Boot `4.0.6`
    - [x] Maven wrapper
    - [x] WebFlux
    - [x] Spring Security
    - [x] Validation
    - [x] R2DBC
    - [x] Flyway
    - [x] Kafka
    - [x] RabbitMQ
    - [x] Actuator
    - [x] Prometheus registry
    - [x] Testcontainers
- [x] Align Go module version with ADR:
    - [x] Update `services/risk-scoring-service/go.mod` from `go 1.25.1` to the accepted Go baseline.
- [x] Add root project README with local setup instructions.
- [x] Add `.gitignore` for Java, Go, IDE, and local platform artifacts.
- [x] Add root development commands document or `Makefile`.
- [x] Add Docker Compose baseline under `platform`.
- [x] Add local infrastructure placeholders:
    - [x] PostgreSQL
    - [x] Redis
    - [x] Kafka
    - [x] RabbitMQ
    - [x] Prometheus
    - [x] Grafana
- [x] Add Spring profiles:
    - [x] `local`
    - [x] `test`
    - [x] `prod`
- [x] Add base Spring package structure by feature:
    - [x] `payment`
    - [x] `merchant`
    - [x] `customer`
    - [x] `risk`
    - [x] `idempotency`
    - [x] `outbox`
    - [x] `audit`
    - [x] `ops`
    - [x] `security`
    - [x] `shared`
- [x] Add Go package structure:
    - [x] `cmd/risk-scoring-service`
    - [x] `internal/config`
    - [x] `internal/grpc`
    - [x] `internal/risk`
    - [x] `internal/health`

### Acceptance Criteria

- [x] `services/payment-orchestrator-service/./mvnw -DskipTests validate` succeeds.
- [x] `services/payment-orchestrator-service/./mvnw test` succeeds.
- [x] `cd services/risk-scoring-service && go test ./...` succeeds.
- [x] Spring Boot app starts locally with the `local` profile.
- [x] `/actuator/health` returns `UP`.
- [x] Go risk service starts and handles graceful shutdown.
- [x] Project setup is documented in the root `README.md`.

## Phase 1: API Contract Baseline

Goal: Define the external REST API, internal gRPC contract, event envelope, and shared API error model before implementing business workflows.

### Steps

- [x] Create shared contract folders:
    - [x] Create `proto/risk/v1`.
    - [x] Create `docs/api`.
    - [x] Create `docs/events`.
- [x] Add protobuf source file:
    - [x] Create `proto/risk/v1/risk_scoring.proto`.
    - [x] Set `syntax = "proto3"`.
    - [x] Set protobuf package to `risk.v1`.
    - [x] Set Java package option.
    - [x] Set Go package option.
- [x] Define protobuf enum contracts:
    - [x] Add `RiskDecision`.
    - [x] Add `RiskReasonCode`.
    - [x] Add stable numeric enum values.
    - [x] Reserve `0` enum values for unspecified states.
- [x] Define protobuf message contracts:
    - [x] Add `ScorePaymentRequest`.
    - [x] Add `ScorePaymentResponse`.
    - [x] Add `RiskRuleHit`.
    - [x] Add amount as integer minor units.
    - [x] Add `correlation_id`.
    - [x] Add `rule_version`.
- [x] Define protobuf service contract:
    - [x] Add `RiskScoringService`.
    - [x] Add unary `ScorePayment` RPC.
    - [x] Document timeout expectation in proto comments.
- [x] Add protobuf tooling documentation:
    - [x] Document required local tools in `README.md`.
    - [x] Document `protoc` installation requirement.
    - [x] Document Go plugins:
        - [x] `protoc-gen-go`
        - [x] `protoc-gen-go-grpc`
- [x] Add Go protobuf generation:
    - [x] Add generated-code target to `Makefile`.
    - [x] Generate Go protobuf messages.
    - [x] Generate Go gRPC service interfaces.
    - [x] Place generated Go files under an agreed package path.
    - [x] Ensure generated Go files compile with `go test ./...`.
- [x] Add Java protobuf generation:
    - [x] Add Maven protobuf plugin.
    - [x] Add Java protobuf runtime dependency.
    - [x] Add Java gRPC dependency.
    - [x] Generate Java protobuf messages.
    - [x] Generate Java gRPC client stubs.
    - [x] Ensure generated Java sources compile with `./mvnw test`.
- [x] Add Spring OpenAPI setup:
    - [x] Add WebFlux-compatible OpenAPI dependency.
    - [x] Configure OpenAPI title.
    - [x] Configure OpenAPI version.
    - [x] Configure server URL for local development.
    - [x] Expose OpenAPI JSON endpoint.
    - [x] Expose Swagger UI endpoint if dependency supports it.
- [ ] Define REST API versioning convention:
    - [ ] Document REST path prefix `/api/v1`.
    - [x] Add package or constant for API v1 base path.
    - [ ] Use `/api/v1` in the first controller.
- [x] Define event schema versioning convention:
    - [x] Create event envelope documentation in `docs/events/event-envelope.md`.
    - [x] Document `schemaVersion`.
    - [x] Document `eventId`.
    - [x] Document `eventType`.
    - [x] Document `aggregateId`.
    - [x] Document `aggregateType`.
    - [x] Document `occurredAt`.
    - [x] Document `producer`.
    - [x] Document `correlationId`.
- [x] Create shared API package structure:
    - [x] `shared/api`
    - [x] `shared/api/error`
    - [x] `shared/api/correlation`
    - [x] `shared/api/version`
- [x] Create global API error response model:
    - [x] Add `ApiErrorResponse`.
    - [x] Add `status`.
    - [x] Add `code`.
    - [x] Add `message`.
    - [x] Add `path`.
    - [x] Add `correlationId`.
    - [x] Add `fieldErrors`.
    - [x] Add `timestamp`.
- [x] Create validation error detail model:
    - [x] Add nested `ApiErrorResponse.FieldError`.
    - [x] Add `field`.
    - [x] Add `message`.
    - [x] Do not expose rejected values in validation responses.
- [x] Define stable API error codes:
    - [x] Add sealed `ApiErrorCode` interface.
    - [x] Add `Business` error code group.
    - [x] Add `Security` error code group.
    - [x] Add `Validation` error code group.
    - [x] Add `Infrastructure` error code group.
    - [x] Add `VALIDATION_FAILED`.
    - [x] Add `RESOURCE_NOT_FOUND`.
    - [x] Add `DUPLICATE_IDEMPOTENCY_KEY`.
    - [x] Add `PAYMENT_STATE_CONFLICT`.
    - [x] Add `RISK_SERVICE_TIMEOUT`.
    - [x] Add `DOWNSTREAM_UNAVAILABLE`.
    - [x] Add `UNAUTHORIZED`.
    - [x] Add `FORBIDDEN`.
    - [x] Add `INTERNAL_ERROR`.
- [x] Add global WebFlux exception handling:
    - [x] Handle Bean Validation errors.
    - [x] Handle request binding errors.
    - [x] Handle malformed request input errors.
    - [x] Handle not-found exceptions.
    - [x] Handle conflict exceptions.
    - [x] Handle downstream timeout exceptions.
    - [x] Handle downstream unavailable exceptions.
    - [x] Handle authentication errors.
    - [x] Handle authorization errors.
    - [x] Handle fallback internal errors.
    - [x] Add WebFlux exception handler tests.
- [ ] Add correlation ID support:
    - [ ] Create correlation ID constant for `X-Correlation-Id`.
    - [ ] Add WebFlux filter.
    - [ ] Accept inbound correlation ID.
    - [ ] Generate missing correlation ID.
    - [ ] Add correlation ID to response headers.
    - [ ] Make correlation ID available to error responses.
    - [ ] Add TODO marker for later gRPC metadata propagation.
    - [ ] Add TODO marker for later Kafka header propagation.
    - [ ] Add TODO marker for later RabbitMQ header propagation.
- [ ] Add first contract-only REST endpoint:
    - [ ] Create a lightweight `GET /api/v1/contract/ping` endpoint.
    - [ ] Return service name.
    - [ ] Return API version.
    - [ ] Return correlation ID.
    - [ ] Use it to validate OpenAPI, errors, and correlation behavior before payment logic exists.
- [ ] Add Spring API tests:
    - [ ] Test contract ping returns `200`.
    - [ ] Test contract ping includes `X-Correlation-Id`.
    - [ ] Test inbound `X-Correlation-Id` is preserved.
    - [ ] Test missing correlation ID is generated.
    - [ ] Test validation failure returns `ApiErrorResponse`.
    - [ ] Test unknown route returns structured error if supported by the handler.
- [ ] Add protobuf contract tests:
    - [ ] Add Go compile test for generated protobuf package.
    - [ ] Add Java compile test for generated protobuf package.
    - [ ] Add one sample `ScorePaymentRequest` construction test in Go.
    - [ ] Add one sample `ScorePaymentRequest` construction test in Java.
- [ ] Add contract documentation:
    - [ ] Document REST conventions in `docs/api/rest-api-conventions.md`.
    - [ ] Document risk gRPC contract in `docs/api/risk-grpc-contract.md`.
    - [ ] Document error response format in `docs/api/error-contract.md`.
    - [ ] Document correlation ID behavior in `docs/api/correlation-id.md`.
- [ ] Update developer commands:
    - [x] Add `make proto`.
    - [ ] Add `make java-run` or keep `make spring-run` documented.
    - [ ] Add `make contract-test` if useful.
    - [ ] Ensure `make test` runs Java and Go checks after generation.

### Acceptance Criteria

- [ ] `proto/risk/v1/risk_scoring.proto` is the single source of truth for the risk gRPC contract.
- [ ] `make proto` generates Go and Java contract code.
- [ ] `make java-test` succeeds after protobuf generation.
- [ ] `make go-test` succeeds after protobuf generation.
- [ ] OpenAPI JSON exposes `GET /api/v1/contract/ping`.
- [ ] `GET /api/v1/contract/ping` returns the API version and correlation ID.
- [ ] Missing correlation IDs are generated.
- [ ] Inbound correlation IDs are preserved.
- [ ] Validation failures return `ApiErrorResponse`.
- [ ] REST, gRPC, event envelope, error, and correlation conventions are documented.

## Phase 2: Payment Authorization API

Goal: Implement the main payment authorization REST flow with validation, idempotency, risk scoring, persistence, and clear state transitions.

### Steps

- [ ] Create payment package structure:
    - [ ] `payment/api`
    - [ ] `payment/api/dto`
    - [ ] `payment/application`
    - [ ] `payment/application/command`
    - [ ] `payment/application/query`
    - [ ] `payment/application/service`
    - [ ] `payment/domain`
    - [ ] `payment/domain/policy`
- [ ] Create payment lifecycle enum:
    - [ ] `RECEIVED`
    - [ ] `RISK_PENDING`
    - [ ] `RISK_APPROVED`
    - [ ] `AUTHORIZED`
    - [ ] `DECLINED`
    - [ ] `REVERSED`
    - [ ] `FAILED`
- [ ] Add authorization request DTO:
    - [ ] `merchantId`
    - [ ] `customerId`
    - [ ] `amountMinor`
    - [ ] `currency`
    - [ ] `paymentMethodToken`
    - [ ] `deviceFingerprint`
    - [ ] `externalReference`
    - [ ] `idempotencyKey`
- [ ] Add authorization response DTO:
    - [ ] `paymentId`
    - [ ] `status`
    - [ ] `authorizationCode`
    - [ ] `riskDecision`
    - [ ] `reasonCodes`
    - [ ] `correlationId`
- [ ] Implement `POST /api/v1/payments/authorize`.
- [ ] Validate authorization requests with Bean Validation and domain policies.
- [ ] Require idempotency keys for authorization.
- [ ] Persist `Payment`, `PaymentAuthorization`, `RiskDecision`, and `IdempotencyRecord`.
- [ ] Call Go risk service through a Java gRPC client.
- [ ] Apply explicit timeout budget to the risk call.
- [ ] Map risk response to payment decision.
- [ ] Store idempotency response snapshots in Redis with TTL.
- [ ] Publish outbox event after successful state transition.
- [ ] Mask sensitive payment and device fields in logs.

### Acceptance Criteria

- [ ] `POST /api/v1/payments/authorize` creates a payment authorization.
- [ ] Duplicate idempotency keys return the original response without creating a second payment.
- [ ] Risk-approved payments can reach `AUTHORIZED`.
- [ ] Risk-declined payments reach `DECLINED`.
- [ ] Risk timeout returns a stable downstream timeout error or fallback decision, depending on policy.
- [ ] Authorization creates an outbox event in the same transaction as payment persistence.
- [ ] Unit tests cover validation, idempotency, state transitions, and risk mapping.
- [ ] API tests cover success, validation failure, duplicate idempotency, and risk timeout paths.

## Phase 3: Payment Lookup And Reversal APIs

Goal: Add read APIs and reversal workflow so payment lifecycle can be inspected and corrected through explicit operations.

### Steps

- [ ] Implement payment lookup:
    - [ ] `GET /api/v1/payments/{paymentId}`
    - [ ] Lookup by internal payment ID
    - [ ] Return payment, authorization, risk, and reversal summary
- [ ] Implement payment reversal:
    - [ ] `POST /api/v1/payments/{paymentId}/reverse`
    - [ ] Require idempotency key
    - [ ] Validate payment is reversible
    - [ ] Reject duplicate or conflicting reversal requests
    - [ ] Persist reversal details
    - [ ] Transition payment to `REVERSED`
    - [ ] Add outbox event
- [ ] Create reversal DTOs:
    - [ ] `ReversePaymentRequest`
    - [ ] `PaymentReversalResponse`
- [ ] Add reversal domain policy:
    - [ ] Authorized payments can be reversed
    - [ ] Declined payments cannot be reversed
    - [ ] Already reversed payments are idempotent or conflict based on request identity
- [ ] Add audit events for lookup and reversal operations.

### Acceptance Criteria

- [ ] `GET /api/v1/payments/{paymentId}` returns payment details.
- [ ] Missing payments return structured not-found errors.
- [ ] `POST /api/v1/payments/{paymentId}/reverse` reverses an authorized payment.
- [ ] Duplicate reversal requests are idempotent.
- [ ] Invalid reversal state returns a structured conflict error.
- [ ] Reversal creates outbox and audit records.

## Phase 4: Go Risk Scoring gRPC Service

Goal: Implement the internal Go risk service with deterministic scoring, rule hits, reason codes, health checks, and graceful shutdown.

### Steps

- [ ] Implement `cmd/risk-scoring-service/main.go`.
- [ ] Load typed configuration from environment variables.
- [ ] Start a gRPC server.
- [ ] Register generated `RiskScoringService`.
- [ ] Implement deterministic risk rules:
    - [ ] High amount rule
    - [ ] Suspicious currency rule
    - [ ] Repeated device rule placeholder
    - [ ] Merchant risk threshold rule
- [ ] Return:
    - [ ] Numeric score
    - [ ] Decision
    - [ ] Reason codes
    - [ ] Rule hits
    - [ ] Rule version
- [ ] Add structured logging with `log/slog`.
- [ ] Add health check endpoint or gRPC health service.
- [ ] Add graceful shutdown for interrupt signals.
- [ ] Add Go unit tests for scoring rules.
- [ ] Add contract-level tests for generated protobuf messages.

### Acceptance Criteria

- [ ] Go service starts locally.
- [ ] Java service can call `ScorePayment`.
- [ ] Risk scoring returns deterministic results for fixed inputs.
- [ ] Rule hits explain why a score was produced.
- [ ] Go tests pass with `go test ./...`.
- [ ] Service shuts down gracefully.

## Phase 5: Operations API

Goal: Add operator-facing REST endpoints for investigation, failed event review, replay, and platform visibility.

### Steps

- [ ] Create ops package structure:
    - [ ] `ops/api`
    - [ ] `ops/api/dto`
    - [ ] `ops/application`
    - [ ] `ops/domain`
- [ ] Implement payment search:
    - [ ] `GET /api/v1/ops/payments`
    - [ ] Filter by status
    - [ ] Filter by merchant
    - [ ] Filter by customer
    - [ ] Filter by created time range
    - [ ] Paginate results
- [ ] Implement outbox inspection:
    - [ ] `GET /api/v1/ops/outbox`
    - [ ] Filter by status
    - [ ] Show retry count, last error, and next retry time
- [ ] Implement dead-letter inspection:
    - [ ] `GET /api/v1/ops/dead-letters`
- [ ] Implement replay command:
    - [ ] `POST /api/v1/ops/replay/{eventId}`
    - [ ] Validate event is replayable
    - [ ] Create replay job
    - [ ] Audit replay request
- [ ] Implement consumer lag view:
    - [ ] `GET /api/v1/ops/consumer-lag`
- [ ] Restrict operations APIs to `OPS` and `ADMIN` roles.

### Acceptance Criteria

- [ ] Operators can search payments by status and date range.
- [ ] Operators can inspect failed outbox events.
- [ ] Operators can inspect dead-letter records.
- [ ] Operators can request replay for eligible events.
- [ ] Operations endpoints enforce role-based authorization.
- [ ] Replay requests create audit events.

## Phase 6: Messaging And Event APIs

Goal: Implement the transactional outbox, Kafka events, consumers, RabbitMQ callback commands, and related operational visibility.

### Steps

- [ ] Define event envelope:
    - [ ] `eventId`
    - [ ] `schemaVersion`
    - [ ] `eventType`
    - [ ] `aggregateId`
    - [ ] `aggregateType`
    - [ ] `occurredAt`
    - [ ] `producer`
    - [ ] `correlationId`
    - [ ] `payload`
- [ ] Define Kafka topics:
    - [ ] `payment.authorization.requested`
    - [ ] `risk.score.completed`
    - [ ] `payment.authorization.completed`
    - [ ] `payment.reversal.completed`
    - [ ] `platform.dead-letter.recorded`
- [ ] Implement outbox relay worker.
- [ ] Implement Kafka producer retry and failure marking.
- [ ] Implement payment audit consumer.
- [ ] Implement settlement projection consumer.
- [ ] Implement ops metrics consumer.
- [ ] Implement idempotent consumer tracking.
- [ ] Implement poison-message dead-letter handling.
- [ ] Define RabbitMQ command:
    - [ ] Queue `partner.callback.commands`
    - [ ] Command `CallPartnerWebhook`
    - [ ] Dead-letter queue `partner.callback.commands.dlq`
- [ ] Implement partner callback worker.
- [ ] Add retry and acknowledgement behavior for callback commands.

### Acceptance Criteria

- [ ] Payment authorization creates Kafka-ready outbox records.
- [ ] Outbox relay publishes events after transaction commit.
- [ ] Audit consumer builds payment history from events.
- [ ] Settlement consumer builds settlement projection rows.
- [ ] Poison Kafka records create dead-letter records.
- [ ] RabbitMQ callback commands are acknowledged only after terminal handling.
- [ ] Callback failures retry and eventually route to DLQ.

## Phase 7: Security, Observability, And Release Readiness

Goal: Harden the APIs for a realistic fintech portfolio demonstration with security controls, metrics, dashboards, and CI checks.

### Steps

- [ ] Configure Spring Security role model:
    - [ ] `MERCHANT`
    - [ ] `OPS`
    - [ ] `AUDITOR`
    - [ ] `ADMIN`
    - [ ] `SERVICE`
- [ ] Add API key or token-based authentication for merchant APIs.
- [ ] Add service-to-service authentication for internal calls.
- [ ] Hash API keys and avoid plaintext secret storage.
- [ ] Add secure headers and CORS defaults.
- [ ] Add request rate limiting by merchant and client.
- [ ] Add metrics:
    - [ ] API latency
    - [ ] Authorization throughput
    - [ ] Decline count by reason
    - [ ] Risk service latency
    - [ ] Risk timeout count
    - [ ] Redis hit/miss rate
    - [ ] Kafka producer failures
    - [ ] Consumer lag
    - [ ] Outbox lag
    - [ ] Dead-letter count
    - [ ] Replay success/failure count
- [ ] Add Prometheus scrape configuration.
- [ ] Add Grafana dashboards.
- [ ] Add CI checks:
    - [ ] Java tests
    - [ ] Go tests
    - [ ] Protobuf generation
    - [ ] Docker Compose validation
    - [ ] Container image build
- [ ] Add Linux operations runbook.
- [ ] Add one incident write-up for a failed risk service or Kafka replay scenario.

### Acceptance Criteria

- [ ] Protected endpoints require authentication.
- [ ] Role-based access rules are enforced.
- [ ] Sensitive data is masked in logs.
- [ ] Prometheus exposes service metrics.
- [ ] Grafana dashboards show API, risk, Kafka, Redis, and database health.
- [ ] CI runs Java, Go, protobuf, and container checks.
- [ ] Runbook documents common production troubleshooting commands.
