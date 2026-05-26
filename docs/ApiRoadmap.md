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

### Chronicle And Next Steps

Phase 2 is intentionally incremental. The project currently has the public API shell, domain model, persistence schema,
row models, repositories, and a contract-only authorization service. It does **not** yet have the complete durable
authorization workflow because payment persistence, database-backed idempotency, Redis replay cache, risk gRPC calls,
outbox creation, and the final transaction boundary still need to be wired together.

#### Completed Foundations

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
- [x] Add persistence models and repositories:
    - Purpose: provide reactive persistence adapters while keeping domain types separate from database row shapes.
  - [x] Add payment row/entity model.
  - [x] Add authorization row/entity model.
  - [x] Add risk decision row/entity model.
  - [x] Add idempotency row/entity model.
  - [x] Add outbox row/entity model.
  - [x] Add reactive payment repository.
  - [x] Add reactive authorization repository.
  - [x] Add reactive risk decision repository.
  - [x] Add reactive idempotency repository.
  - [x] Add reactive outbox repository.
  - [x] Add mapper from domain model to persistence rows.
  - [x] Add mapper from persistence rows to domain model.

#### Current Partial Workflow

- [ ] Complete authorization application service:
  - Purpose: orchestrate validation, idempotency, persistence, risk scoring, state transition, outbox creation, and
    response mapping.
  - [x] Create `AuthorizePaymentService`.
  - [x] Validate command through command/domain value objects.
  - [x] Create a contract-only payment authorization aggregate.
  - [x] Apply a contract-only approved risk decision to payment state.
  - [x] Return response DTO.
  - [x] Check in-memory idempotency before creating a new contract-only authorization.
  - [ ] Persist payment state.
  - [ ] Call risk scoring client.
  - [ ] Persist risk decision.
  - [ ] Persist idempotency result snapshot.
  - [ ] Create outbox event record.
  - [ ] Return stored response from durable idempotency storage when a duplicate request is replayed.
- [ ] Complete idempotency behavior:
    - Purpose: make retries safe by returning the original result for duplicate requests and rejecting conflicting reuse
      of a key.
  - [x] Define idempotency scope for payment authorization.
  - [x] Reject missing idempotency key through request/command validation.
  - [x] Validate idempotency key format and length.
  - [x] Compute stable request fingerprint for authorization commands.
  - [x] Detect duplicate key with same request fingerprint in the current process.
  - [x] Return stored response snapshot for duplicate key with same fingerprint in the current process.
  - [x] Return `IDEMPOTENCY_KEY_CONFLICT` for same key with different fingerprint.
  - [x] Store request fingerprint in the current in-memory implementation.
  - [x] Store response snapshot in the current in-memory implementation.
  - [x] Store idempotency status in the current in-memory implementation.
  - [x] Store expiry time in the current in-memory implementation.
  - [ ] Introduce an idempotency application port/interface so the authorization service does not depend on an
    in-memory implementation.
  - [ ] Persist request fingerprint in `idempotency_records`.
  - [ ] Persist response snapshot in `idempotency_records`.
  - [ ] Persist idempotency status in `idempotency_records`.
  - [ ] Persist expiry time in `idempotency_records`.
    - [ ] Add Redis cache for response snapshot.
    - [ ] Add TTL for Redis snapshot.
    - [ ] Fall back to database idempotency record if Redis misses.

#### Atomic Remaining Work

1. [ ] Introduce idempotency port:
  - Create `IdempotencyStore` or `IdempotencyResultStore` interface in `idempotency/application`.
  - Move lookup/store method contracts behind the interface.
  - Rename the current implementation to `InMemoryIdempotencyStore`.
  - Inject the interface into `DefaultAuthorizePaymentService`.
  - Keep duplicate and conflict unit tests green.
2. [ ] Add idempotency record mapper:
  - Map `IdempotencyScope` to `scope`.
  - Map `IdempotencyKey` to `idempotency_key`.
  - Map request fingerprint to `request_fingerprint`.
  - Map response status to `response_status`.
  - Map response snapshot JSON to `response_body_json`.
  - Map status and expiry fields.
  - Add mapper unit tests.
3. [ ] Add JSON response snapshot serialization:
  - Serialize `AuthorizePaymentResult` to JSON.
  - Deserialize stored JSON back to `AuthorizePaymentResult`.
  - Reject unsupported response snapshot types explicitly.
  - Add stable snapshot round-trip tests.
4. [ ] Add database idempotency read path:
  - Read `idempotency_records` by `(scope, idempotency_key)`.
  - Treat missing records as miss.
  - Treat expired records as miss.
  - Return stored response when fingerprint matches.
  - Throw `IdempotencyKeyConflictException` when fingerprint differs.
  - Add store tests.
5. [ ] Add database idempotency write path:
  - Insert `STARTED` before creating a new payment.
  - Update to `COMPLETED` with response snapshot after successful authorization.
  - Update to `FAILED` or expire when authorization fails before a durable result exists.
  - Preserve unique `(scope, idempotency_key)` behavior.
  - Add duplicate insert race test where practical.
6. [ ] Wire database idempotency into authorization:
  - Use the database-backed idempotency implementation in production wiring.
  - Keep in-memory implementation only for focused tests if useful.
  - Verify duplicate requests do not create a second payment.
  - Verify conflicting requests return `IDEMPOTENCY_KEY_CONFLICT`.
7. [ ] Add sensitive data hashing helpers:
  - Hash `paymentMethodToken` before persistence.
  - Derive token last four for storage where needed.
  - Hash `deviceFingerprint` before persistence.
  - Add deterministic hashing tests.
8. [ ] Add payment state persistence port:
  - Create a payment persistence interface in the payment application boundary.
  - Define save methods for payment, authorization, and risk decision state.
  - Keep the authorization service dependent on the interface, not concrete repositories.
  - Add unit tests with a fake persistence implementation.
9. [ ] Add durable payment write adapter:
  - Save `PaymentRow`.
  - Save `PaymentAuthorizationRow`.
  - Save `PaymentRiskDecisionRow` when a risk decision exists.
  - Use `PaymentPersistenceMapper`.
  - Add adapter tests with mocked repositories.
10. [ ] Wire payment state persistence into authorization:
  - Persist the new payment aggregate after state transition.
  - Persist the current authorization state for the payment.
  - Persist the risk decision attached to the payment.
  - Return response based on the persisted aggregate.
  - Verify one request creates one payment row and one authorization row.
  - Verify authorized and declined outcomes persist the expected state.
11. [ ] Add risk client port:
  - Create risk scoring interface in `risk/application`.
  - Define internal risk request record.
  - Define internal risk response record.
  - Represent approved, declined, review-required, timeout, and unavailable outcomes.
  - Add unit tests with a fake risk client.
12. [ ] Add Java gRPC risk adapter:
  - Create gRPC adapter in `risk/infrastructure/grpc`.
  - Configure risk service host.
  - Configure risk service port.
  - Configure risk call timeout.
  - Map `AuthorizePaymentCommand` or internal request to `ScorePaymentRequest`.
  - Include correlation ID in `ScorePaymentRequest`.
  - Map `ScorePaymentResponse` to internal risk response.
  - Map gRPC deadline exceeded to `RISK_SERVICE_TIMEOUT`.
  - Map unavailable status to `DOWNSTREAM_UNAVAILABLE`.
13. [ ] Add risk decision mapping policy:
  - Map approved risk result to `PaymentRiskDecision`.
  - Map declined risk result to `PaymentRiskDecision`.
  - Define review-required Phase 2 behavior.
  - Define timeout Phase 2 behavior.
  - Preserve risk score, reason codes, rule hit summary, and rule version.
  - Add unit tests for each outcome.
14. [ ] Wire risk client into authorization:
  - Replace contract-only approval with risk client result.
  - Mark payment `AUTHORIZED` for approved result.
  - Mark payment `DECLINED` for declined result.
  - Return stable downstream error or selected fallback for timeout.
  - Return stable downstream error or selected fallback for unavailable.
15. [ ] Add outbox payload records:
  - Add `PaymentAuthorizationRequested` payload.
  - Add `PaymentAuthorized` payload.
  - Add `PaymentDeclined` payload.
  - Include schema version constants.
  - Add payload serialization tests.
16. [ ] Add outbox mapper:
  - Map payment aggregate to event envelope fields.
  - Include `eventId`.
  - Include `correlationId`.
  - Include aggregate type.
  - Include aggregate ID.
  - Include occurred-at timestamp.
  - Add mapper unit tests.
17. [ ] Persist outbox events:
  - Save requested event if selected for Phase 2.
  - Save authorized event when payment is authorized.
  - Save declined event when payment is declined.
  - Mark new events as pending.
  - Add repository tests.
18. [ ] Add reactive transaction boundary:
  - Verify `ReactiveTransactionManager` configuration.
  - Wrap payment rows, idempotency completion update, and outbox insert in one transaction.
  - Avoid holding a transaction open during the remote risk call where practical.
  - Add rollback test for failed outbox insert.
19. [ ] Add Redis idempotency cache adapter:
  - Define Redis key format from scope and idempotency key.
  - Read completed response snapshot from Redis before database lookup.
  - Store completed response snapshot in Redis with TTL.
  - Keep database as source of truth.
  - Add adapter tests.
20. [ ] Add Redis miss database fallback:
  - On Redis miss, read `idempotency_records`.
  - Repopulate Redis from durable database snapshot.
  - Return database snapshot when fingerprint matches.
  - Return conflict when fingerprint differs.
  - Add tests for hit, miss, expired, and conflict paths.
21. [ ] Update authorization API documentation:
  - Document `POST /api/v1/payments/authorize`.
  - Document idempotency key requirements.
  - Document duplicate replay behavior.
  - Document conflict behavior.
  - Document risk timeout/unavailable behavior.
  - Document emitted outbox events.
22. [ ] Add repository/integration tests:
  - Verify Flyway migration applies.
  - Verify payment insert/read.
  - Verify authorization insert/read.
  - Verify risk decision insert/read.
  - Verify idempotency uniqueness.
  - Verify outbox insert with payment transaction.
23. [ ] Add authorization API tests:
  - Valid request returns selected success status.
  - Response includes `paymentId`.
  - Response includes final payment status.
  - Response includes risk decision.
  - Response includes correlation ID.
  - Missing idempotency key returns validation error.
  - Invalid request returns `ApiErrorResponse`.
  - Duplicate idempotency key returns stored response.
  - Idempotency key conflict returns structured conflict error.
  - Risk timeout returns stable downstream timeout error or selected fallback response.

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
