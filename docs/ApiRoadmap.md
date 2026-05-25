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
- [x] Define REST API versioning convention:
    - [x] Document REST path prefix `/api/v1`.
    - [x] Add package or constant for API v1 base path.
    - [x] Use `/api/v1` in the first controller.
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
- [x] Add correlation ID support:
    - [x] Create correlation ID constant for `X-Correlation-Id`.
    - [x] Add WebFlux filter.
    - [x] Accept inbound correlation ID.
    - [x] Generate missing correlation ID.
    - [x] Add correlation ID to response headers.
    - [x] Make correlation ID available to error responses.
    - [x] Add TODO marker for later gRPC metadata propagation.
    - [x] Add TODO marker for later Kafka header propagation.
    - [x] Add TODO marker for later RabbitMQ header propagation.
- [x] Add first contract-only REST endpoint:
    - [x] Create a lightweight `GET /api/v1/contract/ping` endpoint.
    - [x] Return service name.
    - [x] Return API version.
    - [x] Return correlation ID.
    - [x] Use it to validate OpenAPI, errors, and correlation behavior before payment logic exists.
- [x] Add Spring API tests:
    - [x] Test contract ping returns `200`.
    - [x] Test contract ping includes `X-Correlation-Id`.
    - [x] Test inbound `X-Correlation-Id` is preserved.
    - [x] Test missing correlation ID is generated.
    - [x] Test validation failure returns `ApiErrorResponse`.
    - [x] Test unknown route returns structured error if supported by the handler.
- [x] Add protobuf contract tests:
    - [x] Add Go compile test for generated protobuf package.
    - [x] Add Java compile test for generated protobuf package.
    - [x] Add one sample `ScorePaymentRequest` construction test in Go.
    - [x] Add one sample `ScorePaymentRequest` construction test in Java.
- [x] Add contract documentation:
    - [x] Document REST conventions in `docs/api/rest-api-conventions.md`.
    - [x] Document risk gRPC contract in `docs/api/risk-grpc-contract.md`.
    - [x] Document error response format in `docs/api/error-contract.md`.
    - [x] Document correlation ID behavior in `docs/api/correlation-id.md`.
- [x] Update developer commands:
    - [x] Add `make proto`.
  - [x] Add `make java-run` or keep `make spring-run` documented.
  - [x] Add `make contract-test` if useful.
  - [x] Ensure `make test` runs Java and Go checks after generation.

### Acceptance Criteria

- [x] `proto/risk/v1/risk_scoring.proto` is the single source of truth for the risk gRPC contract.
- [x] `make proto` generates Go and Java contract code.
- [x] `make java-test` succeeds after protobuf generation.
- [x] `make go-test` succeeds after protobuf generation.
- [x] OpenAPI JSON exposes `GET /api/v1/contract/ping`.
- [x] `GET /api/v1/contract/ping` returns the API version and correlation ID.
- [x] Missing correlation IDs are generated.
- [x] Inbound correlation IDs are preserved.
- [x] Validation failures return `ApiErrorResponse`.
- [x] REST, gRPC, event envelope, error, and correlation conventions are documented.

## Phase 2: Payment Authorization API

Goal: Implement the main payment authorization REST flow with validation, idempotency, risk scoring, persistence, and clear state transitions.

In this phase, we are building the first real business workflow of the platform: a merchant submits a payment
authorization request, the Java payment orchestrator validates it, protects it with idempotency, asks the Go risk
service for a decision, persists the result, and prepares durable outbox events for later asynchronous processing. The
result should be a thin WebFlux API over a clear domain model, with stable error handling, correlation IDs, and tests
that prove the main authorization paths.

### Steps

- [x] Create payment package structure:
    - Purpose: create the feature boundaries for API, application, domain, and infrastructure code before adding
      behavior.
    - [x] `payment/api`
    - [x] `payment/api/dto`
    - [x] `payment/application`
    - [x] `payment/application/command`
    - [x] `payment/application/query`
    - [x] `payment/application/service`
    - [x] `payment/domain`
    - [x] `payment/domain/policy`
    - [x] `payment/infrastructure/persistence`
    - [x] `payment/infrastructure/risk`
    - [x] `payment/infrastructure/outbox`
- [x] Create idempotency package structure:
    - Purpose: isolate duplicate-request protection from payment business logic so it can later be reused by reversal
      and other command APIs.
    - [x] `idempotency/application`
    - [x] `idempotency/domain`
    - [x] `idempotency/infrastructure/redis`
- [x] Create risk integration package structure:
    - Purpose: separate the internal risk contract and gRPC adapter from payment orchestration code.
    - [x] `risk/application`
    - [x] `risk/infrastructure/grpc`
- [x] Create outbox package structure:
    - Purpose: prepare a clear boundary for durable event records that will be published asynchronously in later phases.
    - [x] `outbox/domain`
    - [x] `outbox/infrastructure/persistence`
- [x] Create payment lifecycle enum:
    - Purpose: define the allowed high-level payment states before implementing transitions.
    - [x] `RECEIVED`
    - [x] `RISK_PENDING`
    - [x] `RISK_APPROVED`
    - [x] `AUTHORIZED`
    - [x] `DECLINED`
    - [x] `REVERSED`
    - [x] `FAILED`
- [x] Create payment domain value objects:
    - Purpose: replace raw strings and numbers with typed, self-validating concepts such as IDs, money, tokens, and
      idempotency keys.
    - [x] `PaymentId`
    - [x] `MerchantId`
    - [x] `CustomerId`
    - [x] `AuthorizationCode`
    - [x] `Money`
    - [x] `PaymentMethodToken`
    - [x] `DeviceFingerprint`
    - [x] `ExternalReference`
    - [x] `IdempotencyKey`
- [x] Create payment domain aggregate/model:
    - Purpose: model payment authorization as domain behavior with explicit state changes instead of scattered
      service-layer mutations.
    - [x] Add `Payment`.
    - [x] Add `PaymentAuthorization`.
    - [x] Add `PaymentRiskDecision`.
    - [x] Add factory for new authorization attempts.
    - [x] Add method to mark payment risk pending.
    - [x] Add method to mark payment authorized.
    - [x] Add method to mark payment declined.
    - [x] Add method to mark payment failed.
- [x] Create payment domain policies:
    - Purpose: centralize business validation rules so controllers and persistence code do not own domain decisions.
    - [x] Validate amount is positive.
    - [x] Validate currency format.
    - [x] Validate merchant ID presence.
    - [x] Validate customer ID presence.
    - [x] Validate payment method token presence.
    - [x] Validate device fingerprint presence.
    - [x] Validate valid authorization state transitions.
- [x] Add authorization request DTO:
    - Purpose: define the public JSON input contract for `POST /api/v1/payments/authorize`.
  - [x] `merchantId`
  - [x] `customerId`
  - [x] `amountMinor`
  - [x] `currency`
  - [x] `paymentMethodToken`
  - [x] `deviceFingerprint`
  - [x] `externalReference`
  - [x] `idempotencyKey`
  - [x] Add Bean Validation annotations.
  - [x] Add OpenAPI schema metadata where useful.
- [x] Add authorization response DTO:
    - Purpose: define the stable public JSON output contract for a payment authorization result.
  - [x] `paymentId`
  - [x] `status`
  - [x] `authorizationCode`
  - [x] `riskDecision`
  - [x] `reasonCodes`
  - [x] `correlationId`
  - [x] `riskScore`
  - [x] `ruleVersion`
  - [x] `createdAt`
- [x] Add payment authorization API shell:
    - Purpose: expose the endpoint with minimal controller logic and delegate all workflow decisions to the application
      service.
  - [x] Create `PaymentAuthorizationController`.
  - [x] Map `POST /api/v1/payments/authorize`.
  - [x] Accept `AuthorizationRequest`.
  - [x] Return `AuthorizationResponse`.
  - [x] Read correlation ID from WebFlux exchange attributes.
  - [x] Delegate to application service only.
- [x] Add authorization command model:
    - Purpose: translate API input into an immutable application command that is independent from transport details.
  - [x] Create `AuthorizePaymentCommand`.
  - [x] Map request DTO to command.
  - [x] Include correlation ID.
  - [x] Include idempotency key.
  - [x] Keep command immutable.
- [x] Add authorization application service:
    - Purpose: orchestrate validation, idempotency, persistence, risk scoring, state transition, outbox creation, and
      response mapping.
  - [x] Create `AuthorizePaymentService`.
  - [x] Validate command through domain policies.
    - [ ] Check idempotency before creating a new authorization.
  - [x] Create new payment authorization aggregate.
    - [ ] Persist payment state.
    - [ ] Call risk scoring client.
  - [x] Apply risk decision to payment state.
    - [ ] Persist risk decision.
    - [ ] Persist idempotency result snapshot.
    - [ ] Create outbox event record.
  - [x] Return response DTO.
- [x] Add persistence migrations:
    - Purpose: create the relational schema needed to durably store authorization state, risk decisions, idempotency
      records, and pending events.
  - [x] Create `payments` table.
  - [x] Create `payment_authorizations` table.
  - [x] Create `payment_risk_decisions` table.
  - [x] Create `idempotency_records` table.
  - [x] Create `outbox_events` table.
  - [x] Add primary keys.
  - [x] Add foreign keys where portable.
  - [x] Add index for `payment_id`.
  - [x] Add index for `merchant_id`.
  - [x] Add index for `customer_id`.
  - [x] Add unique index for idempotency scope and key.
  - [x] Add index for outbox status and next retry time.
- [ ] Add persistence models and repositories:
    - Purpose: provide reactive persistence adapters while keeping domain types separate from database row shapes.
    - [ ] Add payment row/entity model.
    - [ ] Add authorization row/entity model.
    - [ ] Add risk decision row/entity model.
    - [ ] Add idempotency row/entity model.
    - [ ] Add outbox row/entity model.
    - [ ] Add reactive payment repository.
    - [ ] Add reactive authorization repository.
    - [ ] Add reactive risk decision repository.
    - [ ] Add reactive idempotency repository.
    - [ ] Add reactive outbox repository.
    - [ ] Add mapper from domain model to persistence rows.
    - [ ] Add mapper from persistence rows to domain model.
- [ ] Add idempotency behavior:
    - Purpose: make retries safe by returning the original result for duplicate requests and rejecting conflicting reuse
      of a key.
    - [ ] Define idempotency scope for payment authorization.
    - [ ] Reject missing idempotency key.
    - [ ] Validate idempotency key format and length.
    - [ ] Detect duplicate key with same request fingerprint.
    - [ ] Return stored response snapshot for duplicate key with same fingerprint.
    - [ ] Return `IDEMPOTENCY_KEY_CONFLICT` for same key with different fingerprint.
    - [ ] Store request fingerprint.
    - [ ] Store response snapshot.
    - [ ] Store idempotency status.
    - [ ] Store expiry time.
    - [ ] Add Redis cache for response snapshot.
    - [ ] Add TTL for Redis snapshot.
    - [ ] Fall back to database idempotency record if Redis misses.
- [ ] Add Java gRPC risk client:
    - Purpose: connect the Java orchestrator to the Go risk service through the generated protobuf contract with
      explicit timeout handling.
    - [ ] Create risk client interface in application layer.
    - [ ] Create gRPC client adapter.
    - [ ] Configure risk service host.
    - [ ] Configure risk service port.
    - [ ] Configure risk call timeout.
    - [ ] Map `AuthorizePaymentCommand` to `ScorePaymentRequest`.
    - [ ] Include correlation ID in `ScorePaymentRequest`.
    - [ ] Map `ScorePaymentResponse` to internal risk result.
    - [ ] Map approved risk decision.
    - [ ] Map declined risk decision.
    - [ ] Map review-required risk decision.
    - [ ] Map gRPC deadline exceeded to `RISK_SERVICE_TIMEOUT`.
    - [ ] Map unavailable gRPC status to `DOWNSTREAM_UNAVAILABLE`.
- [ ] Add risk decision mapping policy:
    - Purpose: convert risk service responses into payment outcomes and persisted risk decision records.
    - [ ] Approved risk response transitions payment to `AUTHORIZED`.
    - [ ] Declined risk response transitions payment to `DECLINED`.
    - [ ] Review-required risk response uses the selected Phase 2 policy.
    - [ ] Timeout uses the selected Phase 2 policy.
    - [ ] Persist risk score.
    - [ ] Persist reason codes.
    - [ ] Persist rule hits or rule hit summary.
    - [ ] Persist rule version.
- [ ] Add outbox event creation:
    - Purpose: record durable payment facts in the same workflow so later Kafka publishing can be reliable and
      replayable.
    - [ ] Define `PaymentAuthorizationRequested` event payload.
    - [ ] Define `PaymentAuthorized` event payload.
    - [ ] Define `PaymentDeclined` event payload.
    - [ ] Use event envelope fields from `docs/events/event-envelope.md`.
    - [ ] Store outbox event in same transaction as payment state.
    - [ ] Mark new outbox events as pending.
    - [ ] Include correlation ID in outbox event.
    - [ ] Include aggregate ID.
    - [ ] Include aggregate type.
- [ ] Add transaction boundary:
    - Purpose: make database writes and outbox creation consistent while avoiding long-running transactions around
      remote calls.
    - [ ] Use reactive transaction manager.
    - [ ] Wrap payment persistence and outbox insert in one transaction.
    - [ ] Keep remote risk call outside long-running database transaction where practical.
    - [ ] Document chosen transaction order in code or Phase 2 notes.
- [ ] Add sensitive data masking:
    - Purpose: prevent payment tokens and device identifiers from leaking into logs, errors, or operational output.
    - [ ] Do not log raw `paymentMethodToken`.
    - [ ] Do not log full `deviceFingerprint`.
    - [ ] Add masking helper for payment method token.
    - [ ] Add masking helper for device fingerprint.
    - [ ] Ensure API errors do not echo sensitive fields.
- [ ] Add Phase 2 API documentation:
    - Purpose: document the authorization endpoint behavior before Phase 2 is considered complete.
    - [ ] Document `POST /api/v1/payments/authorize`.
    - [ ] Document request fields.
    - [ ] Document response fields.
    - [ ] Document idempotency behavior.
    - [ ] Document risk timeout behavior.
    - [ ] Document emitted outbox events.
- [ ] Add unit tests for domain model:
    - Purpose: prove state transitions and aggregate rules without requiring Spring or infrastructure.
    - [ ] New payment starts in `RECEIVED` or selected initial state.
    - [ ] Payment can transition to `RISK_PENDING`.
    - [ ] Risk-approved payment can transition to `AUTHORIZED`.
    - [ ] Risk-declined payment can transition to `DECLINED`.
    - [ ] Invalid state transition returns conflict/domain error.
- [ ] Add unit tests for validation:
    - Purpose: prove invalid authorization input is rejected before persistence or risk calls.
    - [ ] Missing merchant ID fails.
    - [ ] Missing customer ID fails.
    - [ ] Non-positive amount fails.
    - [ ] Invalid currency fails.
    - [ ] Missing payment method token fails.
    - [ ] Missing idempotency key fails.
- [ ] Add unit tests for idempotency:
    - Purpose: prove duplicate and conflicting authorization requests behave deterministically.
    - [ ] New idempotency key creates a record.
    - [ ] Duplicate key with same fingerprint returns stored response.
    - [ ] Duplicate key with different fingerprint returns conflict.
    - [ ] Redis miss falls back to database.
- [ ] Add unit tests for risk mapping:
    - Purpose: prove gRPC risk outcomes and failures map to stable internal results and API errors.
    - [ ] Approved gRPC response maps to internal approved result.
    - [ ] Declined gRPC response maps to internal declined result.
    - [ ] Review-required gRPC response maps to selected policy result.
    - [ ] gRPC timeout maps to stable timeout error.
    - [ ] gRPC unavailable maps to downstream unavailable error.
- [ ] Add repository/integration tests:
    - Purpose: verify migrations, constraints, and reactive repositories against a real database-backed test setup.
    - [ ] Flyway migration applies successfully.
    - [ ] Payment can be inserted and read.
    - [ ] Authorization can be inserted and read.
    - [ ] Risk decision can be inserted and read.
    - [ ] Idempotency uniqueness is enforced.
    - [ ] Outbox event can be inserted with payment transaction.
- [ ] Add API tests for authorization endpoint:
    - Purpose: prove the complete REST behavior for success, validation, idempotency, conflict, and risk timeout paths.
    - [ ] Valid request returns `200` or selected success status.
    - [ ] Response includes `paymentId`.
    - [ ] Response includes final payment status.
    - [ ] Response includes risk decision.
    - [ ] Response includes correlation ID.
    - [ ] Missing idempotency key returns validation error.
    - [ ] Invalid request returns `ApiErrorResponse`.
    - [ ] Duplicate idempotency key returns stored response.
    - [ ] Idempotency key conflict returns structured conflict error.
    - [ ] Risk timeout returns stable downstream timeout error or selected fallback response.

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
