---
type: project
stack: [java, java-core, j2se, j2ee, spring-boot, webflux, reactor, spring-security, rest, soap, grpc, protobuf, go, microservices, oracle, postgresql, redis, kafka, jms, rabbitmq, docker, kubernetes, testcontainers, micrometer, prometheus, grafana, git, cicd]
repo:
status: planned
created: 2026-05-23
updated: 2026-05-23
tags: [project, java, spring-boot, fintech, payments, reactive, microservices]
---

# Reactive Payment Risk Platform (Java WebFlux + Go gRPC)

## Goal
Build a Java-first payment authorization and risk platform that demonstrates senior backend skills for enterprise Java, fintech, and distributed microservice roles.

The project should prove strong Java fundamentals, modern Spring Boot service design, event-driven architecture, relational persistence, Redis caching, Kafka messaging, security, observability, and polyglot gRPC integration with a small Go microservice.

## Target job fit
- Senior Java backend roles requiring Java Core, J2SE/J2EE concepts, Spring Boot, REST APIs, Oracle-compatible relational design, Redis, Kafka/JMS/MQ, security, testing, and CI/CD.
- Fintech, payment, banking, or mission-critical enterprise roles that value correctness, auditability, reliability, performance, and operational troubleshooting.
- Distributed-system roles that expect asynchronous processing, microservices, event-driven architecture, gRPC/protobuf, Docker/Kubernetes, and production-grade observability.

## Scope
- `payment-orchestrator-service`: Java Spring Boot WebFlux/Reactor service for payment authorization, reversal, lookup, idempotency, event publishing, and operations APIs.
- `risk-scoring-service`: Go gRPC/protobuf microservice that evaluates risk and returns a score, decision, rule hits, and reason codes.
- `event-relay-worker`: publishes transactional outbox records to Kafka and handles retry/dead-letter behavior.
- `payment-audit-consumer`: consumes Kafka payment events and builds an audit/investigation history.
- `settlement-projection-consumer`: consumes Kafka authorization and reversal events to build settlement-ready views.
- `partner-callback-worker`: consumes RabbitMQ/JMS command messages for targeted partner webhook callbacks.
- `ops-api`: operational endpoints for payment investigation, replay, failed events, health, and metrics.
- `local-platform`: Docker Compose setup for PostgreSQL or Oracle-compatible local database, Redis, Kafka, RabbitMQ, Prometheus, Grafana, and the Go service.

## Core domain
### Payment lifecycle
- `RECEIVED`
- `RISK_PENDING`
- `RISK_APPROVED`
- `AUTHORIZED`
- `DECLINED`
- `REVERSED`
- `FAILED`

### Main aggregates and records
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

## Java, J2SE, and J2EE coverage
1. **Java Core and J2SE fundamentals**
   - Use collections, generics, exceptions, records, sealed classes where appropriate, streams, functional interfaces, and immutable value objects.
   - Model domain decisions with clear Java types instead of stringly typed maps.
   - Demonstrate safe resource management, defensive copying, clear exception boundaries, and deterministic validation.

2. **Concurrency and asynchronous programming**
   - Use Reactor for non-blocking request flow, backpressure, timeouts, retries, and composition.
   - Include Java concurrency examples where they fit naturally:
     - `ExecutorService` for bounded background tasks.
     - `CompletableFuture` for bridge code or legacy async adapters.
     - locks, atomics, or bounded queues only where shared-resource coordination is explicit.
   - Document thread pool sizing, event-loop constraints, graceful shutdown, and overload behavior.

3. **JVM fundamentals**
   - Add notes for GC behavior, memory pressure, profiling, thread dumps, heap dumps, and connection pool tuning.
   - Include a troubleshooting runbook for high latency, blocked threads, Kafka lag, Redis failures, and database pool exhaustion.

4. **J2EE-style enterprise concepts**
   - Document servlet/filter-chain awareness even though the main service uses WebFlux.
   - Use Spring Security filters, Bean Validation, dependency injection, AOP-based logging/auditing, transaction boundaries, and JPA/R2DBC tradeoff notes.
   - Include REST APIs and a SOAP compatibility note for legacy enterprise integration.
   - Document JMS/MQ concepts through one small RabbitMQ command flow while Kafka remains the primary event-streaming implementation.

## Reactive Spring design
- Implement APIs with Spring Boot WebFlux and Reactor.
- Keep controller logic thin and push business rules into application/domain services.
- Avoid blocking calls on event-loop threads.
- Use explicit timeout budgets for Redis, database, Kafka, and gRPC calls.
- Apply backpressure and rate limiting to payment authorization endpoints.
- Return stable error contracts for validation, conflicts, duplicate idempotency keys, downstream timeouts, and authorization failures.
- Use correlation IDs across REST, logs, Redis, database records, Kafka events, and gRPC calls.

## Go gRPC risk service
- Implement `risk-scoring-service` in Go to demonstrate polyglot microservice design without weakening the Java focus.
- Expose `RiskScoringService.ScorePayment` through protobuf.
- Keep the Go service small and operationally serious:
  - deterministic rule engine
  - configurable risk thresholds
  - structured logs
  - health checks
  - unit tests
  - graceful shutdown

### Protobuf contract
- `ScorePaymentRequest`
  - `payment_id`
  - `amount`
  - `currency`
  - `merchant_id`
  - `customer_id`
  - `device_fingerprint`
  - `correlation_id`
- `ScorePaymentResponse`
  - `score`
  - `decision`
  - `reason_codes`
  - `rule_hits`
  - `rule_version`

## REST API examples
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

## Kafka event design
Kafka is used for durable business events: facts that already happened and may need to be consumed by multiple services, replayed later, or used to build operational projections. The synchronous payment authorization path does not depend on Kafka to return the initial API response; Kafka handles downstream audit, settlement projections, monitoring, replay, and recovery.

### Topics and events
- `payment.authorization.requested`
  - `PaymentAuthorizationRequested`
- `risk.score.completed`
  - `RiskScoreCompleted`
- `payment.authorization.completed`
  - `PaymentAuthorized`
  - `PaymentDeclined`
- `payment.reversal.completed`
  - `PaymentReversed`
- `platform.dead-letter.recorded`
  - `DeadLetterRecorded`

### Event requirements
- Use stable event IDs, schema versions, occurred timestamps, producer names, correlation IDs, and aggregate IDs.
- Make consumers idempotent.
- Route poison messages to dead-letter topics.
- Track retry count, last error, next retry time, and replay status.
- Document partition keys and ordering expectations for payment IDs and merchant IDs.

### Kafka producers and consumers
- `event-relay-worker`
  - Reads unpublished `OutboxEvent` rows from the relational database.
  - Publishes payment and risk events to Kafka after the business transaction commits.
  - Retries transient publish failures and marks permanently failed rows for operations review.
- `payment-audit-consumer`
  - Consumes `PaymentAuthorizationRequested`, `RiskScoreCompleted`, `PaymentAuthorized`, `PaymentDeclined`, and `PaymentReversed`.
  - Stores audit-friendly event history for investigation, compliance-style review, and troubleshooting.
- `settlement-projection-consumer`
  - Consumes `PaymentAuthorized` and `PaymentReversed`.
  - Builds a settlement-ready read model without coupling settlement logic to the synchronous payment API.
- `ops-metrics-consumer`
  - Consumes payment, risk, and dead-letter events.
  - Updates operational counters for authorization rate, decline reasons, replay outcomes, and dead-letter volume.

## RabbitMQ/JMS command flow
RabbitMQ is used for one targeted command-style workflow where the message means "please do this work" and only one worker should handle each task. This keeps the project honest about JMS/MQ experience without mixing command queues into the Kafka event log.

### Command queue
- Queue: `partner.callback.commands`
- Command: `CallPartnerWebhook`
- Producer: `payment-orchestrator-service`
- Consumer: `partner-callback-worker`

### Flow
1. After a payment reaches `AUTHORIZED`, `DECLINED`, or `REVERSED`, the Java service creates a `CallPartnerWebhook` command for merchants that configured callbacks.
2. `partner-callback-worker` consumes one command at a time, calls the merchant webhook, and acknowledges the message only after a successful delivery or a terminal failure record.
3. Transient failures are retried with backoff.
4. Exhausted messages are routed to a RabbitMQ dead-letter queue such as `partner.callback.commands.dlq`.
5. The operations API exposes callback status, retry count, last error, and manual retry controls.

### Boundary with Kafka
- Kafka publishes durable business facts such as `PaymentAuthorized`.
- RabbitMQ dispatches targeted work such as `CallPartnerWebhook`.
- Kafka events are replayable and may have many independent consumers.
- RabbitMQ commands are short-lived work items intended for one worker group.

## Persistence and cache design
1. **Relational database**
   - Use Oracle-compatible schema design for payments, risk decisions, idempotency records, outbox events, audit events, replay jobs, and dead-letter records.
   - Use PostgreSQL locally if Oracle is not available.
   - Write Flyway or Liquibase migrations with portability in mind.
   - Add indexes for payment ID, merchant ID, customer ID, status, created time, idempotency key, outbox status, and replay job status.

2. **Redis**
   - Store idempotency response snapshots with TTL.
   - Cache risk decisions for safe duplicate payment attempts.
   - Track rate-limit counters by merchant and client.
   - Store hot operational views such as recent failed payments and replay status summaries.
   - Define key naming, TTLs, invalidation behavior, and stale-data rules.

3. **Oracle and SQL talking points**
   - Explain transaction isolation choices.
   - Include reporting queries for daily authorization counts, decline reasons, top risky merchants, and replay outcomes.
   - Document where PL/SQL-style reports or stored procedures would fit in an enterprise deployment.

## Security requirements
- Use Spring Security with role-based access:
  - `MERCHANT`
  - `OPS`
  - `AUDITOR`
  - `ADMIN`
  - `SERVICE`
- Require idempotency keys for payment authorization and reversal.
- Mask sensitive payment, customer, and device fields in logs and audit records.
- Hash API keys and avoid storing plaintext secrets.
- Add audit events for authorization, reversal, replay, admin lookup, and failed authentication.
- Define secure defaults for headers, CORS, rate limiting, and service-to-service credentials.

## Observability and operations
- Use Micrometer, Prometheus, and Grafana.
- Track metrics for:
  - API latency
  - payment authorization throughput
  - risk service latency and timeout count
  - Kafka producer failures
  - Kafka consumer lag
  - outbox lag
  - Redis hit/miss rate
  - database query latency
  - dead-letter count
  - replay success/failure count
- Add structured logs with correlation IDs.
- Add Linux runbook commands for:
  - checking listening ports
  - tailing service logs
  - inspecting Docker Compose containers
  - checking Kafka topics and consumer groups
  - checking Redis keys and TTLs
  - collecting thread dumps and heap dumps

## Testing strategy
- Unit-test Java domain rules, validation, payment state transitions, idempotency logic, risk mapping, and error contracts.
- Mock-test the gRPC risk client, downstream timeouts, retries, and fallback decisions.
- Integration-test REST APIs, database persistence, Kafka producers/consumers, Redis idempotency/cache, and Go gRPC calls.
- Integration-test the RabbitMQ `CallPartnerWebhook` command flow, retry behavior, acknowledgement behavior, and dead-letter routing.
- Use Testcontainers for PostgreSQL, Redis, Kafka, and optionally the Go service.
- Add concurrency tests for duplicate payment authorization and replay.
- Add failure tests for:
  - risk service timeout
  - Redis unavailable
  - Kafka publish failure
  - RabbitMQ callback command retry exhaustion
  - database unique constraint conflict
  - dead-letter recovery
- Add CI checks for Java tests, Go tests, protobuf generation, Docker Compose validation, linting, and container image build.

## Design patterns to demonstrate
- Adapter pattern for the gRPC risk client and any legacy SOAP integration.
- Strategy pattern for risk decision rules and payment authorization policies.
- Chain of responsibility for request validation and fraud/risk checks.
- Transactional outbox for reliable event publishing.
- Idempotent consumer pattern for Kafka handlers.
- Circuit breaker, retry, timeout, and bulkhead patterns for downstream calls.
- Repository pattern for persistence boundaries.
- Command pattern for replay and reversal operations.
- Specification pattern for operational payment search.

## Interview talking points
- Why Java remains the main implementation language for this project even though one small service is written in Go.
- How WebFlux/Reactor differs from traditional servlet-based Spring MVC.
- Where blocking calls can break reactive performance and how to isolate them.
- How J2SE concurrency concepts still matter in modern Spring Boot systems.
- How J2EE concepts such as filters, validation, transactions, dependency injection, and JMS/MQ map to modern Spring applications.
- Why gRPC/protobuf is useful for internal service calls.
- How Redis supports idempotency, rate limiting, and hot operational views.
- How Kafka enables asynchronous event processing, replay, audit, settlement projections, and recovery.
- Why RabbitMQ/JMS is used for targeted command work such as partner callbacks while Kafka is used for durable business events.
- How the outbox pattern prevents lost events.
- How to design Oracle-compatible schemas while using PostgreSQL locally.
- How to investigate high latency, consumer lag, Redis cache misses, and failed replay jobs.

## Deliverables for CV/portfolio
- Architecture diagram with Java service, Go service, Kafka, Redis, database, Prometheus, and Grafana.
- OpenAPI documentation for REST APIs.
- Protobuf contract for the Go risk service.
- Kafka event schema documentation.
- Redis key design.
- Oracle-compatible ERD and migration notes.
- Docker Compose setup.
- Kubernetes manifests or Helm chart.
- CI/CD pipeline.
- Test report and coverage summary.
- Load test and bottleneck analysis.
- Linux operations runbook.
- Incident write-up for one failed risk service or Kafka replay scenario.
- ADRs for:
  - Java WebFlux vs Spring MVC
  - Go gRPC service boundary
  - Kafka event log vs RabbitMQ/JMS command queue
  - Oracle-compatible persistence strategy
  - Redis idempotency and caching strategy
  - outbox and replay strategy

## Suggested implementation phases
1. Define architecture, protobuf contract, REST API contract, and event envelope.
2. Build Java payment domain model, validation, and state transition tests.
3. Build `payment-orchestrator-service` with WebFlux REST APIs and stable error contracts.
4. Add relational persistence, migrations, idempotency records, and audit logs.
5. Add Redis idempotency cache, rate limits, and hot operational views.
6. Build Go `risk-scoring-service` with gRPC/protobuf and unit tests.
7. Integrate Java service with Go risk service using timeout, retry, and fallback policies.
8. Add transactional outbox, Kafka producer, consumers, retries, and dead-letter handling.
9. Add RabbitMQ/JMS `CallPartnerWebhook` command flow with retry, acknowledgement, DLQ, and operations visibility.
10. Add Spring Security roles, service-to-service authentication, masking, and audit events.
11. Add observability, dashboards, Linux runbook, Docker Compose, Kubernetes manifests, and CI/CD.
12. Add Testcontainers integration tests, concurrency tests, failure tests, and load test report.
13. Write ADRs, incident write-up, CV bullets, and interview talking points.

## Suggested CV bullets
- Design a Java Spring Boot WebFlux payment authorization platform with reactive REST APIs, Redis-backed idempotency, Kafka outbox events, Oracle-compatible persistence, and production-grade observability.
- Build a Go gRPC/protobuf risk-scoring microservice integrated with a Java payment orchestrator using timeout, retry, fallback, and correlation-ID propagation.
- Implement event-driven payment workflows with Kafka consumers for audit, settlement projections, metrics, dead-letter handling, replay-safe processing, and Testcontainers-backed integration tests.
- Add a RabbitMQ/JMS command queue for partner webhook callbacks with acknowledgement, retry, dead-letter routing, and operations visibility.
- Document J2SE/J2EE fundamentals through Java concurrency, JVM troubleshooting, Spring Security filters, Bean Validation, transaction boundaries, JMS/MQ concepts, and enterprise API design.
