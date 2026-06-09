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
│   │   ├── event-envelope.md
│   │   └── kafka-topics.md
│   ├── phase-1-api-contract-baseline.md
│   ├── phase-2-payment-authorization.md
│   ├── phase-3-payment-lookup-and-reversal.md
│   ├── phase-4-go-risk-scoring-grpc-service.md
│   ├── phase-5-operations-api.md
│   └── phase-6-messaging-and-event-apis.md
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
    │       │   │   ├── ops
    │       │   │   │   ├── api
    │       │   │   │   │   ├── OpsApiPaths.java
    │       │   │   │   │   ├── OpsFilterParameters.java
    │       │   │   │   │   ├── consumerlag
    │       │   │   │   │   │   ├── OpsConsumerLagController.java
    │       │   │   │   │   │   ├── dto
    │       │   │   │   │   │   │   ├── ConsumerLagItemResponse.java
    │       │   │   │   │   │   │   ├── ConsumerLagResponse.java
    │       │   │   │   │   │   │   └── package-info.java
    │       │   │   │   │   │   └── package-info.java
    │       │   │   │   │   ├── deadletter
    │       │   │   │   │   │   ├── OpsDeadLetterController.java
    │       │   │   │   │   │   ├── OpsDeadLetterInspectionResponseMapper.java
    │       │   │   │   │   │   ├── dto
    │       │   │   │   │   │   │   ├── OpsDeadLetterInspectionResponse.java
    │       │   │   │   │   │   │   ├── OpsDeadLetterItemResponse.java
    │       │   │   │   │   │   │   └── package-info.java
    │       │   │   │   │   │   └── package-info.java
    │       │   │   │   │   ├── dto
    │       │   │   │   │   │   ├── OpsPageRequest.java
    │       │   │   │   │   │   ├── OpsPageResponse.java
    │       │   │   │   │   │   ├── OpsSortDirection.java
    │       │   │   │   │   │   ├── OpsSortRequest.java
    │       │   │   │   │   │   └── package-info.java
    │       │   │   │   │   ├── outbox
    │       │   │   │   │   │   ├── OpsOutboxController.java
    │       │   │   │   │   │   ├── OpsOutboxInspectionResponseMapper.java
    │       │   │   │   │   │   ├── dto
    │       │   │   │   │   │   │   ├── OpsOutboxInspectionItemResponse.java
    │       │   │   │   │   │   │   ├── OpsOutboxInspectionResponse.java
    │       │   │   │   │   │   │   └── package-info.java
    │       │   │   │   │   │   └── package-info.java
    │       │   │   │   │   ├── payment
    │       │   │   │   │   │   ├── OpsPaymentController.java
    │       │   │   │   │   │   ├── OpsPaymentSearchResponseMapper.java
    │       │   │   │   │   │   ├── dto
    │       │   │   │   │   │   │   ├── OpsPaymentSearchItemResponse.java
    │       │   │   │   │   │   │   ├── OpsPaymentSearchResponse.java
    │       │   │   │   │   │   │   └── package-info.java
    │       │   │   │   │   │   └── package-info.java
    │       │   │   │   │   ├── replay
    │       │   │   │   │   │   ├── OpsReplayController.java
    │       │   │   │   │   │   ├── dto
    │       │   │   │   │   │   │   ├── ReplayJobResponse.java
    │       │   │   │   │   │   │   ├── ReplayRequest.java
    │       │   │   │   │   │   │   └── package-info.java
    │       │   │   │   │   │   └── package-info.java
    │       │   │   │   │   └── package-info.java
    │       │   │   │   ├── application
    │       │   │   │   │   ├── consumerlag
    │       │   │   │   │   │   ├── ConsumerLagItem.java
    │       │   │   │   │   │   ├── ConsumerLagPort.java
    │       │   │   │   │   │   ├── ConsumerLagRequest.java
    │       │   │   │   │   │   ├── ConsumerLagResult.java
    │       │   │   │   │   │   ├── ConsumerLagService.java
    │       │   │   │   │   │   ├── ConsumerLagStatus.java
    │       │   │   │   │   │   ├── DefaultConsumerLagService.java
    │       │   │   │   │   │   └── package-info.java
    │       │   │   │   │   ├── deadletter
    │       │   │   │   │   │   ├── DefaultOpsDeadLetterInspectionService.java
    │       │   │   │   │   │   ├── OpsDeadLetterInspectionPort.java
    │       │   │   │   │   │   ├── OpsDeadLetterInspectionRequest.java
    │       │   │   │   │   │   ├── OpsDeadLetterInspectionService.java
    │       │   │   │   │   │   ├── OpsDeadLetterItem.java
    │       │   │   │   │   │   ├── OpsDeadLetterResult.java
    │       │   │   │   │   │   ├── ReplayEligibility.java
    │       │   │   │   │   │   └── package-info.java
    │       │   │   │   │   ├── outbox
    │       │   │   │   │   │   ├── DefaultOpsOutboxInspectionService.java
    │       │   │   │   │   │   ├── OpsOutboxInspectionItem.java
    │       │   │   │   │   │   ├── OpsOutboxInspectionPort.java
    │       │   │   │   │   │   ├── OpsOutboxInspectionRequest.java
    │       │   │   │   │   │   ├── OpsOutboxInspectionResult.java
    │       │   │   │   │   │   ├── OpsOutboxInspectionService.java
    │       │   │   │   │   │   └── package-info.java
    │       │   │   │   │   ├── payment
    │       │   │   │   │   │   ├── DefaultOpsPaymentSearchService.java
    │       │   │   │   │   │   ├── OpsPaymentSearchItem.java
    │       │   │   │   │   │   ├── OpsPaymentSearchPort.java
    │       │   │   │   │   │   ├── OpsPaymentSearchRequest.java
    │       │   │   │   │   │   ├── OpsPaymentSearchResult.java
    │       │   │   │   │   │   ├── OpsPaymentSearchService.java
    │       │   │   │   │   │   └── package-info.java
    │       │   │   │   │   ├── replay
    │       │   │   │   │   │   ├── DefaultReplayRequestService.java
    │       │   │   │   │   │   ├── ReplayAuditPort.java
    │       │   │   │   │   │   ├── ReplayJobResult.java
    │       │   │   │   │   │   ├── ReplayJobStore.java
    │       │   │   │   │   │   ├── ReplayRequestCommand.java
    │       │   │   │   │   │   ├── ReplayRequestService.java
    │       │   │   │   │   │   ├── ReplayTarget.java
    │       │   │   │   │   │   ├── ReplayTargetLookupPort.java
    │       │   │   │   │   │   └── package-info.java
    │       │   │   │   │   └── package-info.java
    │       │   │   │   ├── domain
    │       │   │   │   │   ├── ReplayJob.java
    │       │   │   │   │   ├── ReplayJobId.java
    │       │   │   │   │   ├── ReplayJobStatus.java
    │       │   │   │   │   ├── ReplaySource.java
    │       │   │   │   │   └── package-info.java
    │       │   │   │   ├── infrastructure
    │       │   │   │   │   ├── consumerlag
    │       │   │   │   │   │   ├── UnavailableConsumerLagAdapter.java
    │       │   │   │   │   │   └── package-info.java
    │       │   │   │   │   ├── deadletter
    │       │   │   │   │   │   ├── DatabaseOpsDeadLetterInspectionAdapter.java
    │       │   │   │   │   │   ├── package-info.java
    │       │   │   │   │   │   └── persistence
    │       │   │   │   │   │       ├── DeadLetterRecordEntity.java
    │       │   │   │   │   │       ├── DeadLetterRecordEntityRepository.java
    │       │   │   │   │   │       └── package-info.java
    │       │   │   │   │   ├── outbox
    │       │   │   │   │   │   ├── DatabaseOpsOutboxInspectionAdapter.java
    │       │   │   │   │   │   └── package-info.java
    │       │   │   │   │   ├── payment
    │       │   │   │   │   │   ├── DatabaseOpsPaymentSearchAdapter.java
    │       │   │   │   │   │   └── package-info.java
    │       │   │   │   │   ├── replay
    │       │   │   │   │   │   ├── DatabaseReplayJobStore.java
    │       │   │   │   │   │   ├── DatabaseReplayTargetLookupAdapter.java
    │       │   │   │   │   │   ├── OutboxReplayAuditAdapter.java
    │       │   │   │   │   │   ├── package-info.java
    │       │   │   │   │   │   └── persistence
    │       │   │   │   │   │       ├── ReplayJobEntity.java
    │       │   │   │   │   │       ├── ReplayJobEntityRepository.java
    │       │   │   │   │   │       └── package-info.java
    │       │   │   │   │   └── package-info.java
    │       │   │   │   └── package-info.java
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
    │       │   │   │   │   │   ├── PaymentDetailsResponseMapper.java
    │       │   │   │   │   │   ├── PaymentAuthorizationController.java
    │       │   │   │   │   │   ├── PaymentReversalRequestMapper.java
    │       │   │   │   │   │   ├── PaymentReversalResponseMapper.java
    │       │   │   │   │   │   └── package-info.java
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
    │       │   │   │   │   │   ├── ReversePaymentRequestFingerprint.java
    │       │   │   │   │   │   ├── ReversePaymentResult.java
    │       │   │   │   │   │   └── package-info.java
    │       │   │   │   │   ├── outbox
    │       │   │   │   │   │   ├── PaymentAuthorizationRequestedPayload.java
    │       │   │   │   │   │   ├── PaymentAuthorizedPayload.java
    │       │   │   │   │   │   ├── PaymentDeclinedPayload.java
    │       │   │   │   │   │   ├── PaymentOutboxEventWriter.java
    │       │   │   │   │   │   ├── PaymentOutboxSchemaVersions.java
    │       │   │   │   │   │   ├── PaymentReversedPayload.java
    │       │   │   │   │   │   └── package-info.java
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
    │       │   │   │   │       ├── DefaultPaymentReversalService.java
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
    │       │   │   ├── security
    │       │   │   │   ├── HeaderRoleAuthenticationWebFilter.java
    │       │   │   │   ├── infrastructure/jwt/JwtSecurityConfiguration.java
    │       │   │   │   ├── SecurityRoles.java
    │       │   │   │   └── package-info.java
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
    │       │           ├── V2__create_payment_reversal_tables.sql
    │       │           ├── V3__create_dead_letter_records.sql
    │       │           └── V4__create_ops_replay_jobs.sql
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
    │           ├── ops
    │           │   ├── api
    │           │   │   ├── OpsApiConventionsTest.java
    │           │   │   ├── consumerlag/OpsConsumerLagControllerTest.java
    │           │   │   ├── deadletter/OpsDeadLetterControllerTest.java
    │           │   │   ├── outbox/OpsOutboxControllerTest.java
    │           │   │   ├── payment/OpsPaymentControllerTest.java
    │           │   │   └── replay/OpsReplayControllerTest.java
    │           │   ├── application
    │           │   │   ├── deadletter/OpsDeadLetterResultTest.java
    │           │   │   ├── outbox/OpsOutboxInspectionResultTest.java
    │           │   │   ├── payment
    │           │   │       ├── OpsPaymentSearchRequestTest.java
    │           │   │       └── OpsPaymentSearchResultTest.java
    │           │   │   └── replay/DefaultReplayRequestServiceTest.java
    │           │   ├── domain/ReplayJobTest.java
    │           │   └── infrastructure
    │           │       ├── deadletter
    │           │       │   ├── DatabaseOpsDeadLetterInspectionAdapterTest.java
    │           │       │   └── persistence/DeadLetterRecordEntityRepositoryTest.java
    │           │       ├── outbox/DatabaseOpsOutboxInspectionAdapterTest.java
    │           │       ├── payment/DatabaseOpsPaymentSearchAdapterTest.java
    │           │       └── replay/persistence/ReplayJobEntityRepositoryTest.java
    │           ├── payment
    │           │   ├── api/contract/PaymentAuthorizationControllerTest.java
    │           │   ├── application/service
    │           │   │   ├── AuthorizePaymentResultSnapshotSerializerTest.java
    │           │   │   ├── DefaultAuthorizePaymentServicePersistenceIntegrationTest.java
    │           │   │   ├── DefaultAuthorizePaymentServiceTest.java
    │           │   │   ├── DefaultAuthorizePaymentServiceTransactionTest.java
    │           │   │   ├── DefaultPaymentReversalServicePersistenceIntegrationTest.java
    │           │   │   ├── DefaultPaymentReversalServiceRollbackIntegrationTest.java
    │           │   │   ├── DefaultPaymentReversalServiceTest.java
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
    │           ├── security/SecurityConfigurationTest.java
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
        ├── cmd/risk-scoring-service
        │   ├── main.go
        │   └── main_test.go
        ├── go.mod
        ├── go.sum
        └── internal
            ├── config
            │   ├── config.go
            │   ├── config_test.go
            │   └── doc.go
            ├── go-pointers-cheatsheet.md
            ├── grpc
            │   ├── doc.go
            │   ├── request_validator.go
            │   ├── request_validator_test.go
            │   ├── risk_contract_test.go
            │   ├── risk_mapper.go
            │   ├── risk_mapper_test.go
            │   ├── risk_scoring_integration_test.go
            │   ├── risk_scoring_server.go
            │   └── risk_scoring_server_test.go
            ├── health
            │   ├── doc.go
            │   ├── reporter.go
            │   └── reporter_test.go
            └── risk
                ├── decision_policy.go
                ├── decision_policy_test.go
                ├── doc.go
                ├── high_amount_rule.go
                ├── high_amount_rule_test.go
                ├── low_risk_fallback.go
                ├── low_risk_fallback_test.go
                ├── merchant_risk_threshold_rule.go
                ├── merchant_risk_threshold_rule_test.go
                ├── models.go
                ├── models_test.go
                ├── repeated_device_rule.go
                ├── repeated_device_rule_test.go
                ├── scorer.go
                ├── scorer_test.go
                ├── suspicious_currency_rule.go
                └── suspicious_currency_rule_test.go
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

8. [x] Add payment lookup API:

- [x] Add `GET /api/v1/payments/{paymentId}`.
- [x] Validate payment ID format through domain value object or request validation.
- [x] Map lookup result to response DTO.
- [x] Return structured 404 for missing payment.
- [x] Add API tests for success and missing payment.

9. [x] Add reversal request/response API contract:

- [x] Add `ReversePaymentRequest`.
- [x] Require `idempotencyKey`.
- [x] Include optional `reason`.
- [x] Add Bean Validation annotations.
- [x] Add `PaymentReversalResponse`.
- [x] Include payment ID, reversal ID, status, reason, correlation ID, and reversed-at.
- [x] Add OpenAPI metadata where useful.

10. [x] Add reversal idempotency scope and fingerprint:

- [x] Add `IdempotencyScope.PAYMENT_REVERSAL`.
- [x] Define reversal request fingerprint fields.
- [x] Use payment ID, reason, and any selected command fields in the fingerprint.
- [x] Return stored reversal response for duplicate same-fingerprint requests.
- [x] Return `IDEMPOTENCY_KEY_CONFLICT` for same key with different fingerprint.
- [x] Add unit tests for duplicate and conflicting reversal requests.

11. [x] Add reversal application service:

- [x] Insert `STARTED` idempotency record before new reversal work.
- [x] Load current payment state.
- [x] Validate payment is reversible.
- [x] Create reversal state.
- [x] Persist payment status update and reversal row.
- [x] Complete idempotency record with response snapshot.
- [x] Mark/expire idempotency record on failure before durable completion.
- [x] Add service tests for success, duplicate replay, conflict, missing payment, and invalid state.

12. [x] Add reversal transaction boundary:

- [x] Avoid opening a transaction before idempotency read/miss checks where practical.
- [x] Wrap payment update, reversal insert, idempotency completion, and outbox insert in one transaction.
- [x] Add rollback test for failed outbox insertion.
- [x] Add success-path integration test proving all durable rows commit together.

13. [x] Add reversal outbox payload and mapper:

- [x] Do not add `PaymentReversalRequested` in Phase 3; emit only `PaymentReversed`.
- [x] Add `PaymentReversed` payload.
- [x] Include schema version constants.
- [x] Include correlation ID and aggregate envelope fields.
- [x] Add serialization and mapper tests.

14. [x] Persist reversal outbox events:

- [x] Do not save reversal requested event in Phase 3; save only the final reversed event.
- [x] Save reversed event when reversal succeeds.
- [x] Mark new events as `PENDING`.
- [x] Add outbox repository/service tests.

15. [x] Add reversal API endpoint:

- [x] Add `POST /api/v1/payments/{paymentId}/reverse`.
- [x] Read correlation ID from WebFlux exchange attributes.
- [x] Map request DTO to reversal command.
- [x] Delegate to reversal service.
- [x] Map service result to response DTO.
- [x] Add API tests for success, validation failure, duplicate replay, conflict, not-found, and invalid state.

16. [x] Add audit event placeholders:

- [x] Define lookup audit event shape.
- [x] Define reversal audit event shape.
- [x] Decide whether Phase 3 writes audit rows/events directly or only emits outbox events for later audit consumers.
- [x] Document selected Phase 3 behavior.
- [x] Add tests for selected behavior.

17. [x] Update API documentation:

- [x] Document `GET /api/v1/payments/{paymentId}`.
- [x] Document `POST /api/v1/payments/{paymentId}/reverse`.
- [x] Document reversal idempotency requirements.
- [x] Document duplicate reversal replay behavior.
- [x] Document non-reversible payment conflict behavior.
- [x] Document reversal outbox events.

18. [x] Add Phase 3 integration tests:

- [x] Verify payment lookup after successful authorization.
- [x] Verify reversal after successful authorization.
- [x] Verify duplicate reversal returns original response without second reversal row.
- [x] Verify declined payment reversal returns structured conflict.
- [x] Verify missing payment lookup and reversal return structured 404.
- [x] Verify reversal creates outbox event in the same transaction as payment/reversal persistence.

### Acceptance Criteria

- [x] `GET /api/v1/payments/{paymentId}` returns payment details.
- [x] Missing payments return structured not-found errors.
- [x] `POST /api/v1/payments/{paymentId}/reverse` reverses an authorized payment.
- [x] Duplicate reversal requests are idempotent.
- [x] Invalid reversal state returns a structured conflict error.
- [x] Reversal creates outbox events for later audit consumers.
- [x] Reversal does not expose raw payment method tokens or raw device fingerprints.
- [x] Unit tests cover lookup mapping, reversal policy, idempotency, and state transitions.
- [x] API tests cover lookup success/not-found and reversal success/validation/conflict paths.
- [x] Integration tests cover transaction rollback and durable commit behavior.

## Phase 4: Go Risk Scoring gRPC Service

Goal: Implement the internal Go risk service with deterministic scoring, rule hits, reason codes, health checks, and graceful shutdown.

Detailed implementation guide: `docs/phase-4-go-risk-scoring-grpc-service.md`.

In this phase, we turn the skeletal Go service into the internal risk scoring backend used by the Java payment
orchestrator. The service should expose the generated protobuf `RiskScoringService`, score requests with deterministic
local rules, return explainable rule hits, expose health status, and shut down cleanly.

### Atomic Remaining Work

1. [x] Add Phase 4 Go package and boundary structure:

- [x] Keep `cmd/risk-scoring-service` as the composition root.
- [x] Use `internal/config` for environment parsing and validation.
- [x] Use `internal/risk` for scoring models, rules, thresholds, and scorer behavior.
- [x] Use `internal/grpc` for generated protobuf server adapters only.
- [x] Use `internal/health` for gRPC health behavior.
- [x] Avoid putting scoring logic in `main.go` or protobuf handlers.

2. [x] Implement typed service configuration:

- [x] Parse `RISK_SERVICE_ENV`.
- [x] Parse `RISK_SERVICE_HOST`.
- [x] Parse `RISK_SERVICE_GRPC_PORT`.
- [x] Parse `RISK_RULE_VERSION`.
- [x] Parse `RISK_APPROVE_MAX_SCORE`.
- [x] Parse `RISK_REVIEW_MAX_SCORE`.
- [x] Parse `LOG_LEVEL`.
- [x] Parse `SHUTDOWN_TIMEOUT_SECONDS`.
- [x] Validate required values and numeric ranges.
- [x] Add config tests for defaults, overrides, and invalid values.

3. [x] Configure structured logging:

- [x] Initialize `log/slog` once in `main.go`.
- [x] Support configured log level.
- [x] Include service name and environment in log attributes.
- [x] Log startup, listen address, shutdown start, shutdown completion, and server errors.
- [x] Do not log raw device fingerprint values.

4. [x] Implement risk scoring domain models:

- [x] Add internal request model that mirrors the protobuf fields needed by rules.
- [x] Add internal result model with score, decision, reason codes, rule hits, and rule version.
- [x] Add internal rule hit model with rule ID, reason code, score delta, and message.
- [x] Keep protobuf types out of the core scoring rules.
- [x] Add model tests for decision and rule hit construction where useful.

5. [x] Implement scoring thresholds and decision policy:

- [x] Use `approveMaxScore` to produce `APPROVED`.
- [x] Use `reviewMaxScore` to produce `REVIEW_REQUIRED`.
- [x] Produce `DECLINED` above the review threshold.
- [x] Reject invalid threshold configuration at startup.
- [x] Add policy tests for boundary scores.

6. [x] Implement high amount rule:

- [x] Add a deterministic amount threshold.
- [x] Add a positive score delta when the threshold is exceeded.
- [x] Add `RISK_REASON_CODE_HIGH_AMOUNT`.
- [x] Add a `HIGH_AMOUNT_RULE` rule hit.
- [x] Add unit tests for below-threshold, at-threshold, and above-threshold amounts.

7. [x] Implement suspicious currency rule:

- [x] Add a configured or fixed suspicious currency set for Phase 4.
- [x] Normalize currency casing.
- [x] Add a positive score delta for suspicious currencies.
- [x] Add `RISK_REASON_CODE_SUSPICIOUS_CURRENCY`.
- [x] Add a `SUSPICIOUS_CURRENCY_RULE` rule hit.
- [x] Add unit tests for normal, suspicious, and lowercase currency inputs.

8. [x] Implement repeated device placeholder rule:

- [x] Add deterministic placeholder behavior without external storage.
- [x] Use a clearly documented local heuristic, such as a known test prefix or configured sample list.
- [x] Add `RISK_REASON_CODE_REPEATED_DEVICE`.
- [x] Add a `REPEATED_DEVICE_RULE` rule hit when the placeholder matches.
- [x] Add tests proving the rule is deterministic and does not require database/Redis state.

9. [x] Implement merchant risk threshold rule:

- [x] Add deterministic merchant-risk placeholder behavior without external merchant storage.
- [x] Use a documented fixed list, prefix, or configured sample list.
- [x] Add `RISK_REASON_CODE_MERCHANT_RISK_THRESHOLD_EXCEEDED`.
- [x] Add a `MERCHANT_RISK_THRESHOLD_RULE` rule hit.
- [x] Add tests for low-risk and high-risk merchant inputs.

10. [x] Implement low-risk fallback behavior:

- [x] Return a low-risk reason code when no positive-risk rules match.
- [x] Add `RISK_REASON_CODE_LOW_RISK_PAYMENT`.
- [x] Add a `LOW_RISK_RULE` rule hit or documented fallback explanation.
- [x] Ensure the fallback does not hide positive rule hits.
- [x] Add tests for a clean low-risk request.

11. [x] Implement score aggregation:

- [x] Apply rules in deterministic order.
- [x] Sum all rule score deltas into the final score.
- [x] Deduplicate response reason codes while preserving stable order.
- [x] Include all matching rule hits in response order.
- [x] Include configured rule version in every response.
- [x] Add tests for multiple-rule requests.

12. [x] Implement protobuf mapping:

- [x] Map `ScorePaymentRequest` to internal scoring request.
- [x] Map internal decision to protobuf `RiskDecision`.
- [x] Map internal reason codes to protobuf `RiskReasonCode`.
- [x] Map internal rule hits to protobuf `RiskRuleHit`.
- [x] Map internal result to `ScorePaymentResponse`.
- [x] Add mapper tests for all decisions and reason codes.

13. [x] Implement gRPC risk scoring server:

- [x] Add a server type under `internal/grpc`.
- [x] Embed or implement the generated unimplemented server requirement.
- [x] Register generated `RiskScoringService`.
- [x] Implement unary `ScorePayment`.
- [x] Return protobuf responses from the internal scorer.
- [x] Add gRPC handler tests using an in-memory listener or direct server invocation.

14. [x] Add request validation and gRPC error handling:

- [x] Reject missing `payment_id`.
- [x] Reject non-positive `amount_minor`.
- [x] Reject missing or malformed `currency`.
- [x] Reject missing `merchant_id`.
- [x] Reject missing `customer_id`.
- [x] Return appropriate gRPC status codes for invalid requests.
- [x] Ensure scoring failures do not return partial successful responses.
- [x] Add tests for invalid request errors.

15. [x] Add correlation ID handling:

- [x] Accept correlation ID from the request message.
- [x] Prefer request message correlation ID until metadata propagation is implemented.
- [x] Include correlation ID in request-scoped logs.
- [x] Do not require correlation ID for scoring correctness.
- [x] Add tests or handler assertions for missing and present correlation IDs.

16. [x] Start the gRPC server from `main.go`:

- [x] Build config, logger, scorer, gRPC service, and health service in `main.go`.
- [x] Listen on configured host and port.
- [x] Register `RiskScoringService`.
- [x] Register gRPC health service.
- [x] Start serving without blocking signal handling setup.
- [x] Return startup errors clearly.

17. [x] Implement gRPC health service:

- [x] Register standard gRPC health checks.
- [x] Report `SERVING` after startup.
- [x] Report `NOT_SERVING` during shutdown.
- [x] Add health service tests where practical.

18. [x] Implement graceful shutdown:

- [x] Handle `SIGINT` and `SIGTERM`.
- [x] Stop accepting new requests on shutdown.
- [x] Use configured shutdown timeout.
- [x] Prefer graceful stop before force stop.
- [x] Log shutdown reason and outcome.
- [x] Add unit tests for shutdown helper behavior where practical.

19. [x] Add Go service integration tests:

- [x] Start the gRPC server with an in-process listener.
- [x] Call `ScorePayment` with a low-risk request.
- [x] Call `ScorePayment` with a multi-rule high-risk request.
- [x] Verify health reports `SERVING`.
- [x] Verify invalid requests return gRPC validation errors.
- [x] Keep tests deterministic and free of external infrastructure dependencies.

20. [x] Add Java-to-Go integration verification:

- [x] Add or update local configuration so the Java orchestrator can target the Go service address.
- [x] Add a focused test or documented manual command proving `GrpcRiskScoringClient` can call the running Go service.
- [x] Verify Java mapping still handles approved, review, and declined responses.
- [x] Verify deadline/unavailable behavior remains covered by Java adapter tests.
- [x] Avoid making the normal Java unit test suite require a manually started Go process.

21. [x] Update local runtime and developer docs:

- [x] Update root `README.md` with Go risk service startup commands.
- [x] Update `.env.example` if new configuration values are added.
- [x] Keep `platform/compose.local.yaml` unchanged because Phase 4 runs the Go service with `make risk-run`.
- [x] Document the rule set and scoring examples.
- [x] Document how to run Go tests.

22. [x] Final Phase 4 verification:

- [x] Run `go test ./...` in `services/risk-scoring-service`.
- [x] Run protobuf contract tests.
- [x] Run focused Java risk gRPC adapter tests.
- [x] Verify the Go service starts locally.
- [x] Verify the service shuts down gracefully.
- [x] Update this roadmap and project structure with any added files.

### Acceptance Criteria

- [x] Go service starts locally.
- [x] Java service can call `ScorePayment`.
- [x] Risk scoring returns deterministic results for fixed inputs.
- [x] Rule hits explain why a score was produced.
- [x] Go tests pass with `go test ./...`.
- [x] Service shuts down gracefully.

## Phase 5: Operations API

Goal: Add operator-facing REST endpoints for investigation, failed event review, replay, and platform visibility.

### Atomic Remaining Work

1. [x] Add Phase 5 package and boundary structure:

- [x] Add `ops/api` for WebFlux controllers.
- [x] Add `ops/api/dto` for request and response DTOs.
- [x] Add `ops/application` for query and command services.
- [x] Add `ops/domain` for replay/audit/visibility concepts.
- [x] Add `ops/infrastructure` for database and messaging adapters if needed.
- [x] Keep ops controllers thin and delegate to application services.

2. [x] Define operations API contract conventions:

- [x] Use `/api/v1/ops/**` for operator endpoints.
- [x] Use existing `ApiErrorResponse` for errors.
- [x] Use existing correlation ID behavior.
- [x] Use stable pagination request/response shapes.
- [x] Use stable sort defaults.
- [x] Document filter parameter names.

3. [x] Add ops payment search request model:

- [x] Add status filter.
- [x] Add merchant ID filter.
- [x] Add customer ID filter.
- [x] Add payment ID filter where useful.
- [x] Add created-from and created-to filters.
- [x] Add page size and cursor/page token.
- [x] Validate date range ordering.
- [x] Validate maximum page size.

4. [x] Add ops payment search read model:

- [x] Include payment ID, merchant ID, customer ID, amount, currency, status, and external reference.
- [x] Include authorization summary.
- [x] Include risk summary.
- [x] Include reversal summary when present.
- [x] Include created-at and updated-at.
- [x] Exclude raw payment method tokens and raw device fingerprints.

5. [x] Implement payment search persistence adapter:

- [x] Query payments with optional filters.
- [x] Join or load authorization/risk/reversal summaries.
- [x] Apply stable ordering.
- [x] Apply pagination limits.
- [x] Return empty result pages for no matches.
- [x] Add adapter tests for each filter and combined filters.

6. [x] Add payment search API endpoint:

- [x] Add `GET /api/v1/ops/payments`.
- [x] Bind query parameters into the search request model.
- [x] Delegate to the application query service.
- [x] Map results to DTOs.
- [x] Add API tests for success, validation errors, empty pages, and pagination.

7. [x] Define outbox inspection read model:

- [x] Include event ID, aggregate ID, aggregate type, event type, schema version, and status.
- [x] Include retry count.
- [x] Include last error.
- [x] Include next retry time.
- [x] Include created-at, occurred-at, published-at, and updated-at where available.
- [x] Include correlation ID.
- [x] Exclude full payload by default or define an explicit payload preview policy.

8. [x] Implement outbox inspection adapter:

- [x] Query outbox records by status.
- [x] Query outbox records by event type.
- [x] Query outbox records by aggregate ID.
- [x] Query failed records ordered by next retry time.
- [x] Apply pagination.
- [x] Add repository/adapter tests.

9. [x] Add outbox inspection API endpoint:

- [x] Add `GET /api/v1/ops/outbox`.
- [x] Support status, event type, aggregate ID, and created range filters.
- [x] Show retry count, last error, and next retry time.
- [x] Return structured validation errors.
- [x] Add API tests for success and filter validation.

10. [x] Define dead-letter record model:

- [x] Define dead-letter ID.
- [x] Include source system such as Kafka or RabbitMQ.
- [x] Include topic/queue name.
- [x] Include partition/offset or delivery tag when applicable.
- [x] Include event ID or message ID when available.
- [x] Include failure reason and failure timestamp.
- [x] Include retry/replay eligibility.
- [x] Include correlation ID.

11. [x] Add dead-letter persistence schema:

- [x] Add a Flyway migration for dead-letter records if no table exists.
- [x] Add indexes for source, status, failed-at, and event/message ID.
- [x] Add entity/repository classes.
- [x] Add repository tests proving insert/read/filter behavior.

12. [x] Implement dead-letter inspection API:

- [x] Add `GET /api/v1/ops/dead-letters`.
- [x] Support source, status, topic/queue, event ID, and failed-time filters.
- [x] Return paginated results.
- [x] Hide sensitive payload fields.
- [x] Add API and adapter tests.

13. [x] Define replay job model:

- [x] Add replay job ID.
- [x] Include target event/message ID.
- [x] Include replay source such as outbox or dead-letter.
- [x] Include requested-by principal.
- [x] Include requested-at timestamp.
- [x] Include status such as requested, running, succeeded, failed, rejected.
- [x] Include failure reason when applicable.

14. [x] Add replay job persistence:

- [x] Add Flyway migration for replay jobs if needed.
- [x] Add entity/repository.
- [x] Add unique constraints that prevent duplicate active replay jobs for the same target.
- [x] Add repository tests.

15. [x] Implement replay eligibility policy:

- [x] Define which outbox statuses are replayable.
- [x] Define which dead-letter statuses are replayable.
- [x] Reject already running replay jobs.
- [x] Reject non-existent targets with structured not-found errors.
- [x] Reject terminal non-replayable targets with structured conflict errors.
- [x] Add policy tests.

16. [x] Implement replay command service:

- [x] Add command model for replay requests.
- [x] Validate replay target and source.
- [x] Create replay job.
- [x] Leave target state unchanged until Phase 6 replay execution is implemented.
- [x] Emit an audit outbox event for the replay request.
- [x] Return replay job response.
- [x] Add service tests for success, not-found, conflict, and duplicate active replay.

17. [x] Add replay API endpoint:

- [x] Add `POST /api/v1/ops/replay/{eventId}` or a source-aware replay endpoint.
- [x] Accept optional replay reason.
- [x] Read authenticated operator identity.
- [x] Delegate to replay command service.
- [x] Return replay job details.
- [x] Add API tests for success and structured failures.

18. [x] Add replay audit behavior:

- [x] Define replay audit event shape.
- [x] Include operator identity, target ID, source, reason, correlation ID, and requested-at.
- [x] Persist audit record or emit outbox event according to selected architecture.
- [x] Add tests proving audit is written/emitted on replay request.

19. [x] Define consumer lag read model:

- [x] Include consumer group.
- [x] Include topic.
- [x] Include partition.
- [x] Include current offset, end offset, and lag.
- [x] Include last observed time.
- [x] Include status such as healthy, warning, or critical.

20. [x] Implement consumer lag adapter:

- [x] Define a consumer-lag adapter boundary for Kafka admin/client APIs or a stored metrics projection.
- [x] Handle unavailable Kafka gracefully.
- [x] Return an empty or unavailable status when no consumers exist.
- [x] Add tests with fake adapter/client.

21. [x] Add consumer lag API endpoint:

- [x] Add `GET /api/v1/ops/consumer-lag`.
- [x] Support consumer group and topic filters.
- [x] Return structured unavailable errors or degraded status when Kafka cannot be queried.
- [x] Add API tests.

22. [x] Add operations authorization rules:

- [x] Restrict all `/api/v1/ops/**` endpoints to `OPS` and `ADMIN`.
- [x] Deny merchant-only principals.
- [x] Deny anonymous requests.
- [x] Add security tests for allowed and denied roles.

23. [x] Add Phase 5 documentation and verification:

- [x] Document operations endpoints and filter parameters.
- [x] Update `README.md` if local usage changes.
- [x] Update this roadmap project structure for added files.
- [x] Add focused API, service, repository, and security tests.
- [x] Run the relevant Java test suite.

### Acceptance Criteria

- [x] Operators can search payments by status and date range.
- [x] Operators can inspect failed outbox events.
- [x] Operators can inspect dead-letter records.
- [x] Operators can request replay for eligible events.
- [x] Operations endpoints enforce role-based authorization.
- [x] Replay requests create audit events.

## Phase 6: Messaging And Event APIs

Goal: Implement the transactional outbox, Kafka events, consumers, RabbitMQ callback commands, and related operational visibility.

Detailed implementation guide: `docs/phase-6-messaging-and-event-apis.md`.

### Atomic Remaining Work

1. [x] Verify and finalize event envelope contract:

- [x] Confirm `eventId`.
- [x] Confirm `schemaVersion`.
- [x] Confirm `eventType`.
- [x] Confirm `aggregateId`.
- [x] Confirm `aggregateType`.
- [x] Confirm `occurredAt`.
- [x] Confirm `producer`.
- [x] Confirm `correlationId`.
- [x] Confirm `payload`.
- [x] Add tests for envelope serialization and required fields.

2. [x] Define Kafka topic names and ownership:

- [x] Define `payment.authorization.requested`.
- [x] Define `risk.score.completed`.
- [x] Define `payment.authorization.completed`.
- [x] Define `payment.reversal.completed`.
- [x] Define `platform.dead-letter.recorded`.
- [x] Document producer and consumer ownership for each topic.
- [x] Document partition key selection for each topic.

3. [x] Add Kafka topic configuration:

- [x] Add typed Spring properties for topic names.
- [x] Add local defaults.
- [x] Add production placeholders.
- [x] Add tests for default topic configuration.
- [x] Add optional topic creation/admin configuration if selected.

4. [x] Define outbox relay query model:

- [x] Select pending outbox events.
- [x] Select failed events whose next retry time has arrived.
- [x] Order by creation time.
- [x] Limit batch size.
- [x] Skip locked/in-flight records where supported.
- [x] Add repository tests.

5. [x] Implement outbox claim/lock behavior:

- [x] Mark selected records as in-progress or claimed.
- [x] Store claim timestamp.
- [x] Store relay instance ID if useful.
- [x] Avoid double publishing by concurrent relays.
- [x] Add concurrency-oriented repository tests where practical.

6. [x] Implement Kafka event publisher adapter:

- [x] Map outbox event record to Kafka topic.
- [x] Use aggregate ID or configured key as the Kafka key.
- [x] Add envelope headers such as correlation ID and schema version where useful.
- [x] Publish payload bytes/string without reserializing incorrectly.
- [x] Add producer adapter tests with a fake Kafka template/sender.

7. [x] Implement outbox relay worker:

- [x] Schedule or trigger relay batches.
- [x] Claim eligible records.
- [x] Publish each event.
- [x] Mark success after Kafka acknowledgement.
- [x] Mark failure when publish fails.
- [x] Keep worker idempotent across restarts.
- [x] Add worker tests for success, partial failure, and empty batches.

8. [x] Implement producer retry policy:

- [x] Define max attempts.
- [x] Define retry backoff.
- [x] Compute next retry time.
- [x] Store retry count.
- [x] Store last error.
- [x] Mark terminal failure after max attempts.
- [x] Add retry policy tests.

9. [x] Implement outbox failure marking:

- [x] Mark transient publish failures as retryable.
- [x] Mark terminal publish failures as failed.
- [x] Preserve original event payload.
- [x] Preserve correlation ID.
- [x] Add repository/service tests.

10. [x] Define consumer tracking schema:

- [x] Add processed message/event table if needed.
- [x] Include consumer name.
- [x] Include topic.
- [x] Include partition and offset.
- [x] Include event ID.
- [x] Include processed-at timestamp.
- [x] Add uniqueness constraints for idempotency.
- [x] Add migration and repository tests.

11. [x] Implement idempotent consumer guard:

- [x] Check whether event ID has already been processed by consumer.
- [x] Record successful processing.
- [x] Avoid reprocessing duplicates.
- [x] Handle transaction boundaries around projection writes and processed tracking.
- [x] Add guard tests.

12. [x] Implement payment audit consumer:

- [x] Consume payment lifecycle events.
- [x] Validate envelope schema version.
- [x] Project events into payment audit/history storage.
- [x] Preserve correlation ID and occurred-at.
- [x] Use idempotent consumer guard.
- [x] Add consumer tests for authorized, declined, reversed, duplicate, and invalid messages.

13. [x] Add payment audit persistence:

- [x] Add audit/history table or projection if needed.
- [x] Add entity/repository.
- [x] Add indexes by payment ID and occurred-at.
- [x] Add repository tests.

14. [x] Implement settlement projection consumer:

- [x] Consume authorization and reversal outcome events.
- [x] Build settlement-ready projection rows.
- [x] Update projection on reversal.
- [x] Use idempotent consumer guard.
- [x] Add tests for authorized, declined, reversed, and duplicate events.

15. [x] Add settlement projection persistence:

- [x] Add settlement projection table if needed.
- [x] Add entity/repository.
- [x] Add indexes by merchant, status, and business date.
- [x] Add repository tests.

16. [x] Implement ops metrics consumer:

- [x] Consume selected platform/payment events.
- [x] Update counters/projections for ops views.
- [x] Track event processing failures.
- [x] Use idempotent consumer guard where needed.
- [x] Add tests.

17. [x] Implement poison-message dead-letter handling:

- [x] Detect deserialization failures.
- [x] Detect unsupported schema versions.
- [x] Detect handler exceptions after retries.
- [x] Persist dead-letter record.
- [x] Include topic, partition, offset, key, headers, error, and correlation ID.
- [x] Emit `platform.dead-letter.recorded` if selected.
- [x] Add tests for poison messages.

18. [x] Define RabbitMQ callback command contract:

- [x] Define queue `partner.callback.commands`.
- [x] Define dead-letter queue `partner.callback.commands.dlq`.
- [x] Define `CallPartnerWebhook` command payload.
- [x] Include payment ID, merchant ID, target URL/reference, callback type, attempt, and correlation ID.
- [x] Document acknowledgement and retry behavior.

19. [x] Add RabbitMQ configuration:

- [x] Add queue/exchange/routing-key properties.
- [x] Add local defaults.
- [x] Add queue declaration beans if selected.
- [x] Add DLQ binding configuration.
- [x] Add configuration tests.

20. [x] Implement callback command producer:

- [x] Create callback command after selected payment outcomes.
- [x] Persist command intent or publish through RabbitMQ as selected.
- [x] Include correlation ID.
- [x] Avoid publishing inside an uncommitted database transaction unless using outbox/command table.
- [x] Add producer tests.

21. [x] Implement partner callback worker:

- [x] Consume `CallPartnerWebhook`.
- [x] Call partner webhook/client abstraction.
- [x] Handle success acknowledgement.
- [x] Handle transient failure retry.
- [x] Handle terminal failure.
- [x] Add worker tests with fake partner client.

22. [x] Add RabbitMQ retry and DLQ behavior:

- [x] Define retry count source.
- [x] Define retry delay/backoff.
- [x] Nack/requeue or republish according to selected strategy.
- [x] Route terminal failures to DLQ.
- [x] Add tests for ack, retry, and DLQ paths.

23. [x] Add messaging observability:

- [x] Add outbox lag metric.
- [x] Add publish success/failure metric.
- [x] Add consumer processing metric.
- [x] Add dead-letter count metric.
- [x] Add callback success/failure metric.
- [x] Add tests where practical.

24. [x] Add Phase 6 integration tests:

- [x] Use Kafka Testcontainers for outbox relay publish path.
- [x] Use Kafka Testcontainers for consumer projection path.
- [x] Use RabbitMQ Testcontainers for callback worker path.
- [x] Verify idempotent consumer behavior.
- [x] Verify dead-letter persistence.
- [x] Keep tests isolated and deterministic.

25. [x] Update messaging documentation:

- [x] Document topics.
- [x] Document event payloads.
- [x] Document outbox relay behavior.
- [x] Document consumer idempotency.
- [x] Document RabbitMQ callback behavior.
- [x] Update this roadmap project structure for added files.

### Acceptance Criteria

- [x] Payment authorization creates Kafka-ready outbox records.
- [x] Outbox relay publishes events after transaction commit.
- [x] Audit consumer builds payment history from events.
- [x] Settlement consumer builds settlement projection entities.
- [x] Poison Kafka records create dead-letter records.
- [x] RabbitMQ callback commands are acknowledged only after terminal handling.
- [x] Callback failures retry and eventually route to DLQ.

## Phase 7: Security, Observability, And Release Readiness

Goal: Harden the APIs for a realistic fintech portfolio demonstration with security controls, metrics, dashboards, and CI checks.

Authentication strategy:

- Payment APIs use merchant API keys.
- Ops APIs use JWTs with ops/admin/auditor roles.
- Internal service endpoints use JWTs with the `SERVICE` role.

### Atomic Remaining Work

1. [x] Finalize Spring Security role model:

- [x] Define `MERCHANT`.
- [x] Define `OPS`.
- [x] Define `AUDITOR`.
- [x] Define `ADMIN`.
- [x] Define `SERVICE`.
- [x] Treat `MERCHANT` as the authority resolved from valid merchant API keys.
- [x] Treat `OPS`, `AUDITOR`, `ADMIN`, and `SERVICE` as JWT authorities.
- [x] Document which endpoints each role can access.
- [x] Add role enum/constants where appropriate.

2. [x] Add endpoint authorization matrix:

- [x] Payment APIs require the resolved `MERCHANT` authority; API key resolution is Step 3.
- [x] Ops APIs require a JWT with `OPS` or `ADMIN`.
- [x] Audit read APIs require `AUDITOR`, `OPS`, or `ADMIN`.
- [x] Internal/service endpoints require a JWT with `SERVICE` or `ADMIN`.
- [x] Health/readiness endpoints use selected public/protected behavior.
- [x] Add security tests for each endpoint group.

3. [x] Implement merchant API key authentication:

- [x] Use API keys for payment authorization, lookup, and reversal APIs.
- [x] Define credential header format, for example `X-API-Key`.
- [x] Add API key authentication filter.
- [x] Resolve merchant identity from credential.
- [x] Attach merchant identity to request context.
- [x] Add success and failure tests.

4. [x] Add merchant API key storage model:

- [x] Add merchant API key table or config-backed store.
- [x] Store key ID.
- [x] Store hashed secret.
- [x] Store merchant ID.
- [x] Store active/revoked status.
- [x] Store created-at and rotated-at.
- [x] Add repository tests.

5. [x] Add API key hashing:

- [x] Use strong keyed or salted hashing.
- [x] Avoid plaintext secret storage.
- [x] Add constant-time comparison where applicable.
- [x] Add tests for match, mismatch, and rotation.

6. [x] Implement ops JWT authentication:

- [x] Accept bearer JWTs for `/api/v1/ops/**`.
- [x] Validate issuer, audience, signature, expiration, and role claims.
- [x] Map JWT role claims to `OPS`, `AUDITOR`, and `ADMIN`.
- [x] Keep existing local role-header behavior only as a local/test fallback if retained.
- [x] Add tests for missing, invalid, expired, wrong-audience, and allowed JWTs.

7. [x] Implement internal service JWT authentication:

- [x] Accept bearer JWTs for internal/service endpoints.
- [x] Validate issuer, audience, signature, expiration, and role claims.
- [x] Require `SERVICE` or `ADMIN` for service-only routes.
- [x] Document how internal services obtain and send service JWTs.
- [x] Add tests for allowed and denied service JWTs.

8. [x] Configure secure headers:

- [x] Add content security policy where practical.
- [x] Add frame options.
- [x] Add HSTS where appropriate for production profile.
- [x] Add content type options.
- [x] Add tests or configuration assertions.

9. [x] Configure CORS:

- [x] Define local allowed origins.
- [x] Define production placeholder allowed origins.
- [x] Restrict methods and headers.
- [x] Add preflight tests.

10. [x] Add request rate limiting:

- [x] Define merchant-level limit.
- [x] Define client/IP-level fallback limit for unauthenticated requests.
- [x] Use Redis fixed-window implementation.
- [x] Return structured rate-limit errors.
- [x] Add tests for allowed, exceeded, and reset behavior.

11. [x] Add log masking policy:

- [x] Mask payment method tokens.
- [x] Mask device fingerprints.
- [x] Mask API keys/tokens.
- [x] Mask authorization headers.
- [x] Add tests for log/error masking helpers.

12. [x] Add API latency metrics:

- [x] Record request duration by route and status.
- [x] Avoid high-cardinality labels.
- [x] Add tests or actuator metric assertions.

13. [x] Add payment authorization metrics:

- [x] Count authorization attempts.
- [x] Count authorized outcomes.
- [x] Count declined outcomes.
- [x] Count review-required outcomes.
- [x] Count duplicate idempotency replays.
- [x] Add service-level metric tests where practical.

14. [x] Add decline/risk metrics:

- [x] Count decline by reason code.
- [x] Record risk service latency.
- [x] Count risk timeouts.
- [x] Count risk unavailable responses.
- [x] Add tests with fake meter registry.

15. [x] Add Redis idempotency/cache metrics:

- [x] Count Redis hits.
- [x] Count Redis misses.
- [x] Count Redis write failures.
- [x] Count database fallback hits.
- [x] Add tests where practical.

16. [x] Add messaging metrics:

- [x] Count Kafka producer successes.
- [x] Count Kafka producer failures.
- [x] Record outbox lag.
- [x] Record consumer lag.
- [x] Count dead-letter records.
- [x] Count replay success/failure.
- [x] Add tests or documented dashboard queries.

17. [x] Update Prometheus configuration:

- [x] Scrape Spring Boot actuator metrics.
- [x] Scrape Go risk service metrics if exposed in a later step.
- [x] Keep local Prometheus config aligned with service ports.
- [x] Validate config syntax.

18. [x] Add Grafana dashboards:

- [x] Add API health dashboard.
- [x] Add payment authorization dashboard.
- [x] Add risk service dashboard.
- [x] Add Redis/idempotency dashboard.
- [x] Add Kafka/outbox dashboard.
- [x] Add database health dashboard.
- [x] Store dashboard JSON under platform docs/config.

19. [x] Add CI workflow for Java:

- [x] Run Maven validation.
- [x] Run Java tests.
- [x] Cache Maven dependencies.
- [x] Publish test reports if selected.

20. [x] Add CI workflow for Go:

- [x] Run `go test ./...`.
- [x] Cache Go modules/build cache.
- [x] Run `go vet` if selected.
- [x] Publish test reports if selected.

21. [x] Add CI workflow for protobuf:

- [x] Run `make proto`.
- [x] Fail if generated files are stale.
- [x] Run Java and Go contract tests.

22. [x] Add CI workflow for platform validation:

- [x] Validate Docker Compose config.
- [x] Build service container images if Dockerfiles exist.
- [x] Run lightweight smoke checks where practical.

23. [x] Add container build configuration:

- [x] Add Dockerfile for payment orchestrator if missing.
- [x] Add Dockerfile for risk scoring service if missing.
- [x] Add image build targets to `Makefile`.
- [x] Add CI build checks.

24. [x] Add Linux operations runbook:

- [x] Document local startup and shutdown.
- [x] Document logs inspection.
- [x] Document database checks.
- [x] Document Redis checks.
- [x] Document Kafka checks.
- [x] Document RabbitMQ checks.
- [x] Document replay and dead-letter workflows.

25. [x] Add incident write-up:

- [x] Choose failed risk service or Kafka replay scenario.
- [x] Document impact.
- [x] Document detection signals.
- [x] Document timeline.
- [x] Document mitigation.
- [x] Document prevention/follow-up actions.

26. [x] Add release readiness checklist:

- [x] Verify tests pass.
- [x] Verify docs are current.
- [x] Verify dashboards are available.
- [x] Verify runbook is available.
- [x] Verify secrets are not committed.
- [x] Verify local environment can be started from documented commands.

### Acceptance Criteria

- [x] Protected endpoints require authentication.
- [x] Payment APIs authenticate merchants with API keys.
- [x] Ops APIs authenticate operators with JWT bearer tokens.
- [x] Internal service endpoints authenticate service callers with JWT bearer tokens carrying `SERVICE`.
- [x] Role-based access rules are enforced.
- [x] Sensitive data is masked in logs.
- [x] Prometheus exposes service metrics.
- [x] Grafana dashboards show API, risk, Kafka, Redis, and database health.
- [x] CI runs Java, Go, protobuf, and container checks.
- [x] Runbook documents common production troubleshooting commands.

## Phase 8: Portfolio Polish And Readiness

Goal: Turn the completed payment risk platform into a polished portfolio, resume, and interview artifact that is easy to run, inspect, and discuss.

### Atomic Remaining Work

1. [ ] Add final README overview:

- [ ] State the project purpose in one concise paragraph.
- [ ] Explain the payment risk, idempotency, and event-driven architecture goals.
- [ ] List major capabilities by phase.
- [ ] Include architecture diagram links.
- [ ] Include API, gRPC, event, and persistence contract links.
- [ ] Include local quickstart commands.
- [ ] Include demo script link.
- [ ] Include production-readiness and known-limitations notes.

2. [ ] Add editable visual architecture assets:

- [ ] Add system context diagram for merchant, orchestrator, risk service, Redis, database, Kafka, RabbitMQ, Prometheus, and Grafana.
- [ ] Add payment authorization sequence diagram with idempotency cache and database fallback.
- [ ] Add payment reversal sequence diagram.
- [ ] Add risk scoring gRPC sequence diagram.
- [ ] Add outbox relay and consumer projection sequence diagram.
- [ ] Add RabbitMQ partner callback workflow diagram.
- [ ] Add observability signal flow diagram for logs, metrics, dashboards, and correlation IDs.
- [ ] Prefer Mermaid or editable docs over tracked screenshots.

3. [ ] Add sample API and messaging demo flow:

- [ ] Authorize a payment with a merchant credential.
- [ ] Replay the same authorization with the same idempotency key.
- [ ] Send an idempotency conflict request.
- [ ] Retrieve payment details by payment ID.
- [ ] Reverse an authorized payment.
- [ ] Show declined and review-required risk outcomes.
- [ ] Show outbox event creation and relay status.
- [ ] Show ops payment search results.
- [ ] Show dead-letter inspection or replay path if implemented.
- [ ] Show metrics endpoint or dashboard verification.
- [ ] Keep the primary demo under 10 minutes.

4. [ ] Add incident write-ups:

- [ ] Risk service timeout during authorization.
- [ ] Duplicate authorization request with conflicting payload.
- [ ] Kafka outbox publish failure and replay.
- [ ] Poison message routed to dead-letter handling.
- [ ] Partner callback retry exhaustion.
- [ ] Rate-limit or authentication failure investigation.
- [ ] Include impact, symptoms, detection, root cause, mitigation, and prevention.

5. [ ] Add final test and quality report:

- [ ] Summarize Java unit, slice, integration, and contract tests.
- [ ] Summarize Go unit and gRPC tests.
- [ ] Summarize messaging and Testcontainers coverage.
- [ ] Summarize security and authorization test coverage.
- [ ] Include CI status and checked commands.
- [ ] Include known test gaps and intentionally deferred checks.
- [ ] Include coverage report link or documented coverage command if selected.

6. [ ] Add interview narrative:

- [ ] Add 2-minute project explanation.
- [ ] Add deep-dive talking points for payment authorization flow.
- [ ] Add deep-dive talking points for idempotency and Redis/database fallback.
- [ ] Add deep-dive talking points for gRPC risk scoring.
- [ ] Add deep-dive talking points for outbox, Kafka, and eventual consistency.
- [ ] Add deep-dive talking points for RabbitMQ partner callbacks.
- [ ] Add deep-dive talking points for security, rate limiting, and sensitive-data masking.
- [ ] Add deep-dive talking points for observability and operational troubleshooting.

7. [ ] Add resume bullet points:

- [ ] Java and Spring Boot WebFlux API bullet.
- [ ] Go gRPC risk scoring service bullet.
- [ ] Redis idempotency and cache fallback bullet.
- [ ] PostgreSQL persistence and schema migration bullet.
- [ ] Kafka outbox and event-driven projection bullet.
- [ ] RabbitMQ callback workflow bullet.
- [ ] Security, API key, rate limiting, and log masking bullet.
- [ ] Observability, Prometheus, Grafana, Docker, and CI bullet.
- [ ] Testing and Testcontainers bullet.

8. [ ] Add GitHub project polish:

- [ ] Add repository description guidance.
- [ ] Add topics/tags guidance for Java, Spring Boot, WebFlux, Go, gRPC, Kafka, RabbitMQ, Redis, PostgreSQL, Docker, fintech, and observability.
- [ ] Add contribution note or portfolio disclaimer.
- [ ] Add roadmap completion summary.
- [ ] Add known limitations and next steps.
- [ ] Verify license and public sharing expectations.
- [ ] Verify README badges are accurate if used.

9. [ ] Add portfolio readiness checklist:

- [ ] Verify local startup works from documented commands.
- [ ] Verify demo commands match current API paths and DTOs.
- [ ] Verify generated protobuf files are current.
- [ ] Verify diagrams link to existing files.
- [ ] Verify docs do not include secrets or private credentials.
- [ ] Verify examples use safe local credentials only.
- [ ] Verify logs and error examples do not expose sensitive payment data.
- [ ] Verify repository structure in this roadmap is current.

### Test Scenarios

- [ ] Demo flow:
    - [ ] Demo starts from clean local seed data.
    - [ ] Authorization returns approved, declined, or review-required outcomes as expected.
    - [ ] Idempotency replay returns the original response.
    - [ ] Idempotency conflict returns a structured error.
    - [ ] Reversal updates payment state and emits the expected event.
    - [ ] Outbox relay publishes or exposes pending status as documented.
    - [ ] Ops search finds the demo payment by merchant, status, and time range.
    - [ ] Metrics show authorization, risk, idempotency, and messaging activity.
- [ ] Documentation links:
    - [ ] README links to ADRs.
    - [ ] README links to API and gRPC contracts.
    - [ ] README links to event contracts.
    - [ ] README links to diagrams.
    - [ ] README links to runbook and incident write-ups.
    - [ ] All referenced files exist.
- [ ] Interview artifacts:
    - [ ] 2-minute explanation is concise and specific.
    - [ ] Deep dives connect design decisions to fintech reliability concerns.
    - [ ] Resume bullets are concrete and technology-specific.
    - [ ] Incident write-ups include detection and prevention, not only fixes.
- [ ] Repository hygiene:
    - [ ] No secrets in docs, examples, or committed config.
    - [ ] No private machine paths in public-facing docs unless clearly marked as local examples.
    - [ ] CI badge is correct if present.
    - [ ] Demo commands match current service ports and profiles.
    - [ ] Portfolio docs clearly separate implemented features from future work.

### Acceptance Criteria

- [ ] Project can be explained in a 2-minute interview answer.
- [ ] README clearly states the fintech payment-risk goals and architecture.
- [ ] Demo flow proves authorization, idempotency, risk scoring, reversal, events, and ops visibility.
- [ ] Diagrams make the service boundaries and async messaging paths easy to inspect.
- [ ] Incident write-ups demonstrate troubleshooting and operational thinking.
- [ ] Resume bullets highlight Java, Spring Boot WebFlux, Go, gRPC, Redis, PostgreSQL, Kafka, RabbitMQ, Docker, security, observability, and testing.
- [ ] Repository is easy to run, review, and discuss as a portfolio project.
- [ ] Release-readiness checklist verifies tests, docs, diagrams, demo commands, and secret hygiene.
