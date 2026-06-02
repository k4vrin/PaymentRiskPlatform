# Reactive Payment Risk Platform API Roadmap

This roadmap breaks the API and service work into practical development phases. Each phase should leave the project in a working, reviewable state with clear acceptance criteria.

Use the checkboxes as the implementation tracker.

## Current Project Structure

Update this section after each implementation step that adds, removes, renames, or moves files.

```text
.
├── LICENSE
├── Makefile
├── README.md
├── docs
│   ├── ApiRoadmap.md
│   ├── Project.md
│   ├── adr
│   │   ├── 0001-lock-project-stack-and-dependencies.md
│   │   ├── 0002-use-kafka-and-rabbitmq-for-distinct-messaging-needs.md
│   │   └── 0003-use-prefixed-application-generated-identifiers.md
│   ├── api
│   │   ├── correlation-id.md
│   │   ├── error-contract.md
│   │   ├── rest-api-conventions.md
│   │   └── risk-grpc-contract.md
│   ├── events
│   │   └── event-envelope.md
│   ├── phase-1-api-contract-baseline.md
│   ├── phase-2-payment-authorization.md
│   └── phase-3-payment-lookup-and-reversal.md
├── platform
│   ├── compose.local.yaml
│   └── prometheus
│       └── prometheus.yml
├── proto
│   ├── gen
│   │   └── go
│   │       ├── go.mod
│   │       └── risk
│   │           └── v1
│   │               ├── risk_scoring.pb.go
│   │               └── risk_scoring_grpc.pb.go
│   └── risk
│       └── v1
│           └── risk_scoring.proto
└── services
    ├── payment-orchestrator-service
    │   ├── .env.example
    │   ├── mvnw
    │   ├── mvnw.cmd
    │   ├── pom.xml
    │   └── src
    │       ├── main
    │       │   ├── java/dev/kavrin/paymentrisk
    │       │   │   ├── PaymentOrchestratorServiceApplication.java
    │       │   │   ├── audit/package-info.java
    │       │   │   ├── customer/package-info.java
    │       │   │   ├── idempotency
    │       │   │   │   ├── application
    │       │   │   │   │   ├── IdempotencyResultStore.java
    │       │   │   │   │   ├── InMemoryIdempotencyResultStore.java
    │       │   │   │   │   ├── ScopedIdempotencyKey.java
    │       │   │   │   │   ├── StoredIdempotencyResult.java
    │       │   │   │   │   └── package-info.java
    │       │   │   │   ├── domain
    │       │   │   │   │   ├── IdempotencyKey.java
    │       │   │   │   │   ├── IdempotencyKeyConflictException.java
    │       │   │   │   │   ├── IdempotencyScope.java
    │       │   │   │   │   ├── IdempotencyStatus.java
    │       │   │   │   │   └── package-info.java
    │       │   │   │   ├── infrastructure
    │       │   │   │   │   ├── package-info.java
    │       │   │   │   │   ├── persistence
    │       │   │   │   │   │   ├── CompletedIdempotencyResult.java
    │       │   │   │   │   │   ├── DatabaseIdempotencyResultStore.java
    │       │   │   │   │   │   ├── DatabaseIdempotencyResultOperations.java
    │       │   │   │   │   │   ├── IdempotencyRecordMapper.java
    │       │   │   │   │   │   ├── IdempotencyRecordEntity.java
    │       │   │   │   │   │   └── IdempotencyRecordEntityRepository.java
    │       │   │   │   │   └── redis
    │       │   │   │   │       ├── CachedIdempotencySnapshot.java
    │       │   │   │   │       ├── RedisIdempotencyKeyFormatter.java
    │       │   │   │   │       ├── RedisIdempotencySnapshotCache.java
    │       │   │   │   │       ├── RedisIdempotencySnapshotSerializer.java
    │       │   │   │   │       ├── SpringRedisIdempotencySnapshotCache.java
    │       │   │   │   │       └── package-info.java
    │       │   │   │   └── package-info.java
    │       │   │   ├── merchant/package-info.java
    │       │   │   ├── ops/package-info.java
    │       │   │   ├── outbox
    │       │   │   │   ├── domain/package-info.java
    │       │   │   │   ├── infrastructure
    │       │   │   │   │   ├── package-info.java
    │       │   │   │   │   └── persistence/package-info.java
    │       │   │   │   └── package-info.java
    │       │   │   ├── payment
    │       │   │   │   ├── api
    │       │   │   │   │   ├── contract
    │       │   │   │   │   │   ├── AuthorizationRequestMapper.java
    │       │   │   │   │   │   ├── AuthorizationResponseMapper.java
    │       │   │   │   │   │   └── PaymentAuthorizationController.java
    │       │   │   │   │   ├── dto
    │       │   │   │   │   │   ├── AuthorizationRequest.java
    │       │   │   │   │   │   ├── AuthorizationResponse.java
    │       │   │   │   │   │   ├── PaymentDetailsResponse.java
    │       │   │   │   │   │   ├── PaymentReversalResponse.java
    │       │   │   │   │   │   ├── ReversePaymentRequest.java
    │       │   │   │   │   │   └── package-info.java
    │       │   │   │   │   └── package-info.java
    │       │   │   │   ├── application
    │       │   │   │   │   ├── command
    │       │   │   │   │   │   ├── AuthorizePaymentCommand.java
    │       │   │   │   │   │   ├── AuthorizePaymentResult.java
    │       │   │   │   │   │   ├── ReversePaymentCommand.java
    │       │   │   │   │   │   ├── ReversePaymentResult.java
    │       │   │   │   │   │   └── package-info.java
    │       │   │   │   │   ├── outbox
    │       │   │   │   │   │   ├── PaymentAuthorizationRequestedPayload.java
    │       │   │   │   │   │   ├── PaymentAuthorizedPayload.java
    │       │   │   │   │   │   ├── PaymentDeclinedPayload.java
    │       │   │   │   │   │   ├── PaymentOutboxEventWriter.java
    │       │   │   │   │   │   └── PaymentOutboxSchemaVersions.java
    │       │   │   │   │   ├── package-info.java
    │       │   │   │   │   ├── query
    │       │   │   │   │   │   ├── DefaultPaymentLookupService.java
    │       │   │   │   │   │   ├── PaymentDetailsLookupPort.java
    │       │   │   │   │   │   ├── PaymentDetailsResult.java
    │       │   │   │   │   │   ├── PaymentLookupService.java
    │       │   │   │   │   │   └── package-info.java
    │       │   │   │   │   └── service
    │       │   │   │   │       ├── AuthorizePaymentResultSnapshotSerializer.java
    │       │   │   │   │       ├── AuthorizePaymentService.java
    │       │   │   │   │       ├── DefaultAuthorizePaymentService.java
    │       │   │   │   │       ├── PaymentReversalPersistencePort.java
    │       │   │   │   │       ├── PaymentReversalService.java
    │       │   │   │   │       ├── PaymentStatePersistencePort.java
    │       │   │   │   │       ├── RiskDecisionMappingPolicy.java
    │       │   │   │   │       └── package-info.java
    │       │   │   │   ├── domain
    │       │   │   │   │   ├── model
    │       │   │   │   │   │   ├── AuthorizationCode.java
    │       │   │   │   │   │   ├── CustomerId.java
    │       │   │   │   │   │   ├── DeviceFingerprint.java
    │       │   │   │   │   │   ├── ExternalReference.java
    │       │   │   │   │   │   ├── MerchantId.java
    │       │   │   │   │   │   ├── Money.java
    │       │   │   │   │   │   ├── Payment.java
    │       │   │   │   │   │   ├── PaymentAuthorization.java
    │       │   │   │   │   │   ├── PaymentId.java
    │       │   │   │   │   │   ├── PaymentMethodToken.java
    │       │   │   │   │   │   ├── PaymentRiskDecision.java
    │       │   │   │   │   │   ├── PaymentStateTransitionException.java
    │       │   │   │   │   │   ├── PaymentStatus.java
    │       │   │   │   │   │   ├── RequiredText.java
    │       │   │   │   │   │   ├── RiskDecision.java
    │       │   │   │   │   │   └── package-info.java
    │       │   │   │   │   ├── package-info.java
    │       │   │   │   │   └── policy/package-info.java
    │       │   │   │   ├── infrastructure
    │       │   │   │   │   ├── outbox
    │       │   │   │   │   │   ├── DatabasePaymentOutboxEventWriter.java
    │       │   │   │   │   │   ├── PaymentOutboxEventMapper.java
    │       │   │   │   │   │   └── package-info.java
    │       │   │   │   │   ├── package-info.java
    │       │   │   │   │   ├── persistence
    │       │   │   │   │   │   ├── DatabasePaymentDetailsLookupAdapter.java
    │       │   │   │   │   │   ├── DurablePaymentStatePersistenceAdapter.java
    │       │   │   │   │   │   ├── PaymentPersistenceMapper.java
    │       │   │   │   │   │   ├── SensitivePaymentDataHasher.java
    │       │   │   │   │   │   ├── entities
    │       │   │   │   │   │   │   ├── OutboxEventEntity.java
    │       │   │   │   │   │   │   ├── PaymentAuthorizationEntity.java
    │       │   │   │   │   │   │   ├── PaymentRiskDecisionEntity.java
    │       │   │   │   │   │   │   ├── PaymentReversalEntity.java
    │       │   │   │   │   │   │   ├── PaymentEntity.java
    │       │   │   │   │   │   │   └── package-info.java
    │       │   │   │   │   │   ├── package-info.java
    │       │   │   │   │   │   └── repository
    │       │   │   │   │   │       ├── OutboxEventEntityRepository.java
    │       │   │   │   │   │       ├── PaymentAuthorizationEntityRepository.java
    │       │   │   │   │   │       ├── PaymentRiskDecisionEntityRepository.java
    │       │   │   │   │   │       ├── PaymentReversalEntityRepository.java
    │       │   │   │   │   │       ├── PaymentEntityRepository.java
    │       │   │   │   │   │       └── package-info.java
    │       │   │   │   │   └── risk/package-info.java
    │       │   │   │   └── package-info.java
    │       │   │   ├── risk
    │       │   │   │   ├── application
    │       │   │   │   │   ├── RiskScoringClient.java
    │       │   │   │   │   ├── dto
    │       │   │   │   │   │   ├── RiskScoringOutcome.java
    │       │   │   │   │   │   ├── RiskScoringRequest.java
    │       │   │   │   │   │   ├── RiskScoringResponse.java
    │       │   │   │   │   │   └── RiskRuleHitSummary.java
    │       │   │   │   │   └── package-info.java
    │       │   │   │   ├── infrastructure
    │       │   │   │   │   ├── grpc
    │       │   │   │   │   │   ├── GrpcRiskScoringClient.java
    │       │   │   │   │   │   ├── RiskGrpcConfiguration.java
    │       │   │   │   │   │   ├── RiskGrpcProperties.java
    │       │   │   │   │   │   └── package-info.java
    │       │   │   │   │   └── package-info.java
    │       │   │   │   └── package-info.java
    │       │   │   ├── security/package-info.java
    │       │   │   └── shared
    │       │   │       ├── api
    │       │   │       │   ├── contract/ContractPingController.java
    │       │   │       │   ├── correlation
    │       │   │       │   │   ├── CorrelationIdWebFilter.java
    │       │   │       │   │   ├── CorrelationIds.java
    │       │   │       │   │   └── package-info.java
    │       │   │       │   ├── error
    │       │   │       │   │   ├── ApiErrorCode.java
    │       │   │       │   │   ├── ApiErrorResponse.java
    │       │   │       │   │   ├── ConflictException.java
    │       │   │       │   │   ├── DownstreamTimeoutException.java
    │       │   │       │   │   ├── DownstreamUnavailableException.java
    │       │   │       │   │   ├── GlobalApiExceptionHandler.java
    │       │   │       │   │   ├── PaymentRiskApiException.java
    │       │   │       │   │   ├── ResourceNotFoundException.java
    │       │   │       │   │   └── package-info.java
    │       │   │       │   ├── package-info.java
    │       │   │       │   └── version
    │       │   │       │       ├── ApiPaths.java
    │       │   │       │       └── package-info.java
    │       │   │       ├── config
    │       │   │       │   ├── JacksonObjectMapperConfiguration.java
    │       │   │       │   ├── SystemClockConfiguration.java
    │       │   │       │   └── package-info.java
    │       │   │       ├── id/PlatformIdGeneratorFactory.java
    │       │   │       └── package-info.java
    │       │   └── resources
    │       │       ├── application-local.yaml
    │       │       ├── application-prod.yaml
    │       │       ├── application-test.yaml
    │       │       ├── application.yaml
    │       │       └── db/migration
    │       │           ├── V1__create_payment_authorization_tables.sql
    │       │           └── V2__create_payment_reversal_tables.sql
    │       └── test/java/dev/kavrin/paymentrisk
    │           ├── PaymentOrchestratorServiceApplicationTests.java
    │           ├── TestPostgresConfiguration.java
    │           ├── TestRedisConfiguration.java
    │           ├── TestPaymentOrchestratorServiceApplication.java
    │           ├── TestcontainersConfiguration.java
    │           ├── idempotency/infrastructure
    │           │   ├── persistence
    │           │   │   ├── DatabaseIdempotencyResultStoreTest.java
    │           │   │   └── IdempotencyRecordMapperTest.java
    │           │   └── redis
    │           │       ├── RedisIdempotencyKeyFormatterTest.java
    │           │       └── SpringRedisIdempotencySnapshotCacheTest.java
    │           ├── payment
    │           │   ├── api/contract/PaymentAuthorizationControllerTest.java
    │           │   ├── application/service
    │           │   │   ├── AuthorizePaymentResultSnapshotSerializerTest.java
    │           │   │   ├── DefaultAuthorizePaymentServicePersistenceIntegrationTest.java
    │           │   │   ├── DefaultAuthorizePaymentServiceTest.java
    │           │   │   ├── DefaultAuthorizePaymentServiceTransactionTest.java
    │           │   │   ├── PaymentStatePersistencePortTest.java
    │           │   │   └── RiskDecisionMappingPolicyTest.java
    │           │   ├── domain/PaymentDomainValueObjectsTest.java
    │           │   └── infrastructure
    │           │       ├── outbox
    │           │       │   ├── DatabasePaymentOutboxEventWriterTest.java
    │           │       │   └── PaymentOutboxEventMapperTest.java
    │           │       └── persistence
    │           │           ├── DatabasePaymentDetailsLookupAdapterTest.java
    │           │           ├── DurablePaymentStatePersistenceAdapterTest.java
    │           │           ├── PaymentPersistenceMapperTest.java
    │           │           ├── repository
    │           │           │   ├── OutboxEventEntityRepositoryTest.java
    │           │           │   └── PaymentReversalEntityRepositoryTest.java
    │           │           └── SensitivePaymentDataHasherTest.java
    │           ├── risk
    │           │   ├── application/RiskScoringClientTest.java
    │           │   └── infrastructure/grpc/GrpcRiskScoringClientTest.java
    │           └── shared
    │               ├── api
    │               │   ├── contract
    │               │   │   ├── ContractPingControllerTest.java
    │               │   │   ├── OpenApiContractTest.java
    │               │   │   └── RiskGrpcContractTest.java
    │               │   ├── correlation/CorrelationIdWebFilterTest.java
    │               │   └── error
    │               │       ├── GlobalApiExceptionHandlerTest.java
    │               │       └── TestErrorController.java
    │               └── id/PlatformIdGeneratorFactoryTest.java
    └── risk-scoring-service
        ├── .env.example
        ├── cmd/risk-scoring-service/main.go
        ├── go.mod
        ├── go.sum
        └── internal
            ├── config/doc.go
            ├── grpc
            │   ├── doc.go
            │   └── risk_contract_test.go
            ├── health/doc.go
            └── risk/doc.go
```

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

Detailed implementation guide: `docs/phase-2-payment-authorization.md`.

In this phase, we are building the first real business workflow of the platform: a merchant submits a payment
authorization request, the Java payment orchestrator validates it, protects it with idempotency, asks the Go risk
service for a decision, persists the result, and prepares durable outbox events for later asynchronous processing. The
result should be a thin WebFlux API over a clear domain model, with stable error handling, correlation IDs, and tests
that prove the main authorization paths.

### Chronicle And Next Steps

Phase 2 is complete. The project now has the public API, domain model, persistence schema, entity models,
repositories, durable authorization workflow, Redis replay cache, risk gRPC adapter, outbox creation, and transaction
boundary wired together with focused unit, API, repository, and integration tests.

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
    - Purpose: provide reactive persistence adapters while keeping domain types separate from database entity shapes.
    - [x] Add payment entity model.
    - [x] Add authorization entity model.
    - [x] Add risk decision entity model.
    - [x] Add idempotency entity model.
    - [x] Add outbox entity model.
    - [x] Add reactive payment repository.
    - [x] Add reactive authorization repository.
    - [x] Add reactive risk decision repository.
    - [x] Add reactive idempotency repository.
    - [x] Add reactive outbox repository.
    - [x] Add mapper from domain model to persistence entities.
    - [x] Add mapper from persistence entities to domain model.

#### Current Partial Workflow

- [x] Complete authorization application service:
    - Purpose: orchestrate validation, idempotency, persistence, risk scoring, state transition, outbox creation, and
      response mapping.
    - [x] Create `AuthorizePaymentService`.
    - [x] Validate command through command/domain value objects.
    - [x] Create a contract-only payment authorization aggregate.
    - [x] Apply a contract-only approved risk decision to payment state.
    - [x] Return response DTO.
    - [x] Check database idempotency before creating a new contract-only authorization.
    - [x] Insert a `STARTED` idempotency record before creating a new contract-only authorization.
    - [x] Complete the idempotency record with the serialized response snapshot after authorization succeeds.
    - [x] Return stored response from durable idempotency storage when a duplicate request is replayed.
    - [x] Create a payment state persistence port.
    - [x] Add a durable payment write adapter that maps payment state to database entities.
    - [x] Persist payment state.
    - [x] Call risk scoring client.
    - [x] Persist risk decision.
    - [x] Create outbox event record.
- [x] Complete idempotency behavior:
    - Purpose: make retries safe by returning the original result for duplicate requests and rejecting conflicting reuse
      of a key.
    - [x] Define idempotency scope for payment authorization.
    - [x] Reject missing idempotency key through request/command validation.
    - [x] Validate idempotency key format and length.
    - [x] Compute stable request fingerprint for authorization commands.
    - [x] Detect duplicate key with same request fingerprint.
    - [x] Return stored response snapshot for duplicate key with same fingerprint.
    - [x] Return `IDEMPOTENCY_KEY_CONFLICT` for same key with different fingerprint.
    - [x] Store request fingerprint in the current in-memory implementation.
    - [x] Store response snapshot in the current in-memory implementation.
    - [x] Store idempotency status in the current in-memory implementation.
    - [x] Store expiry time in the current in-memory implementation.
    - [x] Introduce an idempotency application port/interface so the authorization service does not depend on an
          in-memory implementation.
    - [x] Persist request fingerprint in `idempotency_records`.
    - [x] Persist response snapshot in `idempotency_records`.
    - [x] Persist idempotency status in `idempotency_records`.
    - [x] Persist expiry time in `idempotency_records`.
    - [x] Add Redis cache for response snapshot.
    - [x] Add TTL for Redis snapshot.
    - [x] Fall back to database idempotency record if Redis misses.

#### Atomic Remaining Work

1. [x] Introduce idempotency port:

- [x] Create `IdempotencyResultStore` interface in `idempotency/application`.
- [x] Move lookup/store method contracts behind the interface.
- [x] Rename the current implementation to `InMemoryIdempotencyResultStore`.
- [x] Inject the interface into `DefaultAuthorizePaymentService`.
- [x] Keep duplicate and conflict unit tests green.

2. [x] Add idempotency record mapper:

- [x] Map `IdempotencyScope` to `scope`.
- [x] Map `IdempotencyKey` to `idempotency_key`.
- [x] Map request fingerprint to `request_fingerprint`.
- [x] Map response status to `response_status`.
- [x] Map response snapshot JSON to `response_body_json`.
- [x] Map status and expiry fields.
- [x] Add mapper unit tests.

3. [x] Add JSON response snapshot serialization:

- Serialize `AuthorizePaymentResult` to JSON.
- Deserialize stored JSON back to `AuthorizePaymentResult`.
- Reject unsupported response snapshot types explicitly.
- Add stable snapshot round-trip tests.

4. [x] Add database idempotency read path:

- [x] Read `idempotency_records` by `(scope, idempotency_key)`.
- [x] Treat missing records as miss.
- [x] Treat expired records as miss.
- [x] Return stored response when fingerprint matches.
- [x] Throw `IdempotencyKeyConflictException` when fingerprint differs.
- [x] Add store tests.
- [x] Wire the read path as a normal Spring bean backed directly by `IdempotencyRecordEntityRepository`.
- [x] Use PostgreSQL-backed tests so the read path uses the same repository and schema shape as production.

5. [x] Add database idempotency write path:

- [x] Insert `STARTED` before creating a new payment.
- [x] Update to `COMPLETED` with response snapshot after successful authorization.
- [x] Update to `FAILED` or expire when authorization fails before a durable result exists.
- [x] Preserve unique `(scope, idempotency_key)` behavior.
- [x] Add duplicate insert uniqueness/race test where practical.

6. [x] Wire database idempotency into authorization:

- [x] Use the database-backed idempotency implementation in production wiring.
- [x] Keep in-memory implementation out of the authorization service wiring.
- [x] Verify duplicate replay returns the stored database response instead of creating a new contract-only payment.
- [x] Verify conflicting requests return `IDEMPOTENCY_KEY_CONFLICT`.

7. [x] Add sensitive data hashing helpers:

- [x] Hash `paymentMethodToken` before persistence.
- [x] Derive token last four for storage where needed.
- [x] Hash `deviceFingerprint` before persistence.
- [x] Add deterministic hashing tests.

8. [x] Add payment state persistence port:

- [x] Create a payment persistence interface in the payment application boundary.
- [x] Define a save method for the payment aggregate, including authorization and risk decision state.
- [x] Keep the persistence boundary as an interface so authorization wiring can avoid concrete repositories.
- [x] Add unit tests with a fake persistence implementation.

9. [x] Add durable payment write adapter:

- [x] Save `PaymentEntity`.
- [x] Save `PaymentAuthorizationEntity`.
- [x] Save `PaymentRiskDecisionEntity` when a risk decision exists.
- [x] Use `PaymentPersistenceMapper`.
- [x] Add adapter tests with mocked database inserts.

10. [x] Wire payment state persistence into authorization:

- [x] Persist the new payment aggregate after state transition.
- [x] Persist the current authorization state for the payment.
- [x] Persist the risk decision attached to the payment.
- [x] Return response based on the persisted aggregate.
- [x] Verify one request saves one payment aggregate through the persistence port.
- [x] Verify the durable adapter writes payment, authorization, and risk decision entities for the current contract-only authorized outcome.

11. [x] Add risk client port:

- [x] Create risk scoring interface in `risk/application`.
- [x] Define internal risk request record.
- [x] Define internal risk response record.
- [x] Represent approved, declined, review-required, timeout, and unavailable outcomes.
- [x] Add unit tests with a fake risk client.

12. [x] Add Java gRPC risk adapter:

- [x] Create gRPC adapter in `risk/infrastructure/grpc`.
- [x] Configure risk service host.
- [x] Configure risk service port.
- [x] Configure risk call timeout.
- [x] Map `AuthorizePaymentCommand` or internal request to `ScorePaymentRequest`.
- [x] Include correlation ID in `ScorePaymentRequest`.
- [x] Map `ScorePaymentResponse` to internal risk response.
- [x] Map gRPC deadline exceeded to `RISK_SERVICE_TIMEOUT`.
- [x] Map unavailable status to `DOWNSTREAM_UNAVAILABLE`.

13. [x] Add risk decision mapping policy:

- [x] Map approved risk result to `PaymentRiskDecision`.
- [x] Map declined risk result to `PaymentRiskDecision`.
- [x] Define review-required Phase 2 behavior.
- [x] Define timeout Phase 2 behavior.
- [x] Preserve risk score, reason codes, rule hit summary, and rule version.
- [x] Add unit tests for each outcome.

14. [x] Wire risk client into authorization:

- [x] Replace contract-only approval with risk client result.
- [x] Mark payment `AUTHORIZED` for approved result.
- [x] Mark payment `DECLINED` for declined result.
- [x] Return stable downstream error or selected fallback for timeout.
- [x] Return stable downstream error or selected fallback for unavailable.

15. [x] Add outbox payload records:

- [x] Add `PaymentAuthorizationRequested` payload.
- [x] Add `PaymentAuthorized` payload.
- [x] Add `PaymentDeclined` payload.
- [x] Include schema version constants.
- [x] Add payload serialization tests.

16. [x] Add outbox mapper:

- [x] Map payment aggregate to event envelope fields.
- [x] Include `eventId`.
- [x] Include `correlationId`.
- [x] Include aggregate type.
- [x] Include aggregate ID.
- [x] Include occurred-at timestamp.
- [x] Add mapper unit tests.

17. [x] Persist outbox events:

- [x] Save requested event if selected for Phase 2.
- [x] Save authorized event when payment is authorized.
- [x] Save declined event when payment is declined.
- [x] Mark new events as pending.
- [x] Add repository tests.

18. [x] Add reactive transaction boundary:

- [x] Verify `ReactiveTransactionManager` configuration.
- [x] Wrap payment entities, idempotency completion update, and outbox insert in one transaction.
- [x] Avoid holding a transaction open during the remote risk call where practical.
- [x] Add rollback test for failed outbox insert.
- [x] Add success-path integration test verifying payment, authorization, risk decision, outbox, and completed
      idempotency rows are persisted.
- [x] Use explicit `R2dbcEntityTemplate` inserts for new application-assigned ID rows.

19. [x] Add Redis idempotency cache adapter:

- [x] Define Redis key format from scope and idempotency key.
- [x] Read completed response snapshot from Redis before database lookup.
- [x] Store completed response snapshot in Redis with TTL.
- [x] Keep database as source of truth.
- [x] Add adapter tests.
- [x] Add Redis Testcontainer coverage for store, lookup, and expiry.

20. [x] Add Redis miss database fallback:

- [x] On Redis miss, read `idempotency_records`.
- [x] Repopulate Redis from durable database snapshot.
- [x] Return database snapshot when fingerprint matches.
- [x] Return conflict when fingerprint differs.
- [x] Add tests for hit, miss, expired, and conflict paths.

21. [x] Update authorization API documentation:

- [x] Document `POST /api/v1/payments/authorize`.
- [x] Document idempotency key requirements.
- [x] Document duplicate replay behavior.
- [x] Document conflict behavior.
- [x] Document risk timeout/unavailable behavior.
- [x] Document emitted outbox events.

22. [x] Add repository/integration tests:

- [x] Verify Flyway migration applies.
- [x] Verify payment insert/read.
- [x] Verify authorization insert/read.
- [x] Verify risk decision insert/read.
- [x] Verify idempotency uniqueness.
- [x] Verify outbox insert with payment transaction.

23. [x] Add authorization API tests:

- [x] Valid request returns selected success status.
- [x] Response includes `paymentId`.
- [x] Response includes final payment status.
- [x] Response includes risk decision.
- [x] Response includes correlation ID.
- [x] Missing idempotency key returns validation error.
- [x] Invalid request returns `ApiErrorResponse`.
- [x] Duplicate idempotency key returns stored response.
- [x] Idempotency key conflict returns structured conflict error.
- [x] Risk timeout returns stable downstream timeout error or selected fallback response.

### Acceptance Criteria

- [x] `POST /api/v1/payments/authorize` creates a payment authorization.
- [x] Duplicate idempotency keys return the original response without creating a second payment.
- [x] Risk-approved payments can reach `AUTHORIZED`.
- [x] Risk-declined payments reach `DECLINED`.
- [x] Risk timeout returns a stable downstream timeout error or fallback decision, depending on policy.
- [x] Authorization creates an outbox event in the same transaction as payment persistence.
- [x] Unit tests cover validation, idempotency, state transitions, and risk mapping.
- [x] API tests cover success, validation failure, duplicate idempotency, and risk timeout paths.

## Phase 3: Payment Lookup And Reversal APIs

Goal: Add read APIs and reversal workflow so payment lifecycle can be inspected and corrected through explicit operations.

Detailed implementation guide: `docs/phase-3-payment-lookup-and-reversal.md`.

In this phase, we add the read side needed to inspect a payment and the first corrective command: reversal. Lookup
should expose a stable, non-sensitive view of the payment, authorization, risk, and reversal state. Reversal should be a
separate command workflow with idempotency protection, strict domain-state validation, durable persistence, and outbox
events written in the same transaction as the state change.

### Atomic Remaining Work

1. [x] Add Phase 3 package and boundary structure:

- [x] Add `payment/application/query` query models for payment details.
- [x] Add `payment/application/query` port/service for payment lookup.
- [x] Add `payment/application/command` models for payment reversal.
- [x] Add `payment/application/service` reversal service boundary.
- [x] Add `payment/api/dto` lookup response DTOs.
- [x] Add `payment/api/dto` reversal request/response DTOs.
- [x] Keep controllers thin and delegate to application services only.

2. [x] Add reversal persistence schema:

- [x] Add Flyway migration for `payment_reversals`.
- [x] Include reversal ID, payment ID, merchant/customer IDs where useful, reversal reason, idempotency key, status,
      requested-at, reversed-at, created-at, and updated-at.
- [x] Add primary key and payment foreign key.
- [x] Add unique index for reversal idempotency scope/key or payment/key as selected.
- [x] Add lookup index for `payment_id`.
- [x] Add repository/entity tests proving the migration applies.

3. [x] Add reversal domain model and policy:

- [x] Add `PaymentReversal` domain concept or equivalent value object.
- [x] Add `ReversalId`.
- [x] Add `ReversalReason`.
- [x] Define allowed reversal states.
- [x] Define that `AUTHORIZED` payments can be reversed.
- [x] Define that `DECLINED`, `FAILED`, and `RECEIVED` payments cannot be reversed.
- [x] Define behavior for already `REVERSED` payments.
- [x] Add domain tests for reversible, non-reversible, and already-reversed cases.

4. [x] Extend payment aggregate for reversal:

- [x] Add method to mark an authorized payment reversed.
- [x] Preserve original authorization details after reversal.
- [x] Record reversal timestamp and reason.
- [x] Reject invalid state transitions with `PaymentStateTransitionException`.
- [x] Add aggregate state-transition tests.

5. [x] Add reversal persistence model and mapper:

- [x] Add `PaymentReversalEntity`.
- [x] Add `PaymentReversalEntityRepository`.
- [x] Map reversal domain/application model to entity.
- [x] Add persistence mapper tests.
- [x] Add repository tests for insert/read by payment ID.

6. [x] Add payment lookup read model:

- [x] Define `PaymentDetailsResult`.
- [x] Include payment ID, merchant ID, customer ID, amount, currency, status, external reference, created-at,
      updated-at.
- [x] Include authorization status/code/timestamps.
- [x] Include risk decision, score, reason codes, rule version, decided-at.
- [x] Include reversal summary when present.
- [x] Exclude raw payment token and raw device fingerprint.

7. [x] Implement payment lookup read adapter:

- [x] Query `payments` by payment ID.
- [x] Query authorization by payment ID.
- [x] Query risk decision by payment ID when present.
- [x] Query reversal by payment ID when present.
- [x] Return `ResourceNotFoundException` or equivalent for missing payment.
- [x] Add adapter tests for full, partial, and missing records.

8. [ ] Add payment lookup API:

- [ ] Add `GET /api/v1/payments/{paymentId}`.
- [ ] Validate payment ID format through domain value object or request validation.
- [ ] Map lookup result to response DTO.
- [ ] Return structured 404 for missing payment.
- [ ] Add API tests for success and missing payment.

9. [ ] Add reversal request/response API contract:

- [ ] Add `ReversePaymentRequest`.
- [ ] Require `idempotencyKey`.
- [ ] Include optional `reason`.
- [ ] Add Bean Validation annotations.
- [ ] Add `PaymentReversalResponse`.
- [ ] Include payment ID, reversal ID, status, reason, correlation ID, and reversed-at.
- [ ] Add OpenAPI metadata where useful.

10. [ ] Add reversal idempotency scope and fingerprint:

- [ ] Add `IdempotencyScope.PAYMENT_REVERSAL`.
- [ ] Define reversal request fingerprint fields.
- [ ] Use payment ID, reason, and any selected command fields in the fingerprint.
- [ ] Return stored reversal response for duplicate same-fingerprint requests.
- [ ] Return `IDEMPOTENCY_KEY_CONFLICT` for same key with different fingerprint.
- [ ] Add unit tests for duplicate and conflicting reversal requests.

11. [ ] Add reversal application service:

- [ ] Insert `STARTED` idempotency record before new reversal work.
- [ ] Load current payment state.
- [ ] Validate payment is reversible.
- [ ] Create reversal state.
- [ ] Persist payment status update and reversal row.
- [ ] Complete idempotency record with response snapshot.
- [ ] Mark/expire idempotency record on failure before durable completion.
- [ ] Add service tests for success, duplicate replay, conflict, missing payment, and invalid state.

12. [ ] Add reversal transaction boundary:

- [ ] Avoid opening a transaction before idempotency read/miss checks where practical.
- [ ] Wrap payment update, reversal insert, idempotency completion, and outbox insert in one transaction.
- [ ] Add rollback test for failed outbox insertion.
- [ ] Add success-path integration test proving all durable rows commit together.

13. [ ] Add reversal outbox payload and mapper:

- [ ] Add `PaymentReversalRequested` payload if selected for Phase 3.
- [ ] Add `PaymentReversed` payload.
- [ ] Include schema version constants.
- [ ] Include correlation ID and aggregate envelope fields.
- [ ] Add serialization and mapper tests.

14. [ ] Persist reversal outbox events:

- [ ] Save reversal requested event if selected.
- [ ] Save reversed event when reversal succeeds.
- [ ] Mark new events as `PENDING`.
- [ ] Add outbox repository/service tests.

15. [ ] Add reversal API endpoint:

- [ ] Add `POST /api/v1/payments/{paymentId}/reverse`.
- [ ] Read correlation ID from WebFlux exchange attributes.
- [ ] Map request DTO to reversal command.
- [ ] Delegate to reversal service.
- [ ] Map service result to response DTO.
- [ ] Add API tests for success, validation failure, duplicate replay, conflict, not-found, and invalid state.

16. [ ] Add audit event placeholders:

- [ ] Define lookup audit event shape.
- [ ] Define reversal audit event shape.
- [ ] Decide whether Phase 3 writes audit rows/events directly or only emits outbox events for later audit consumers.
- [ ] Document selected Phase 3 behavior.
- [ ] Add tests for selected behavior.

17. [ ] Update API documentation:

- [ ] Document `GET /api/v1/payments/{paymentId}`.
- [ ] Document `POST /api/v1/payments/{paymentId}/reverse`.
- [ ] Document reversal idempotency requirements.
- [ ] Document duplicate reversal replay behavior.
- [ ] Document non-reversible payment conflict behavior.
- [ ] Document reversal outbox events.

18. [ ] Add Phase 3 integration tests:

- [ ] Verify payment lookup after successful authorization.
- [ ] Verify reversal after successful authorization.
- [ ] Verify duplicate reversal returns original response without second reversal row.
- [ ] Verify declined payment reversal returns structured conflict.
- [ ] Verify missing payment lookup and reversal return structured 404.
- [ ] Verify reversal creates outbox event in the same transaction as payment/reversal persistence.

### Acceptance Criteria

- [ ] `GET /api/v1/payments/{paymentId}` returns payment details.
- [ ] Missing payments return structured not-found errors.
- [ ] `POST /api/v1/payments/{paymentId}/reverse` reverses an authorized payment.
- [ ] Duplicate reversal requests are idempotent.
- [ ] Invalid reversal state returns a structured conflict error.
- [ ] Reversal creates outbox and audit records.
- [ ] Reversal does not expose raw payment method tokens or raw device fingerprints.
- [ ] Unit tests cover lookup mapping, reversal policy, idempotency, and state transitions.
- [ ] API tests cover lookup success/not-found and reversal success/validation/conflict paths.
- [ ] Integration tests cover transaction rollback and durable commit behavior.

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
- [ ] Settlement consumer builds settlement projection entities.
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
