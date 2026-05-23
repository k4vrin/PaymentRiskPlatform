# ADR 0001: Lock Project Stack and Dependency Baseline

## Status
Accepted

## Date
2026-05-23

## Context
The Reactive Payment Risk Platform is a Java-first portfolio project intended to demonstrate senior backend engineering for fintech, payments, and distributed systems roles. The project needs a stable technology baseline before implementation starts so API contracts, persistence choices, messaging boundaries, observability, tests, and deployment artifacts are built against one coherent stack.

The stack must support:
- Reactive Java payment APIs with Spring Boot WebFlux and Reactor.
- Oracle-compatible relational modeling while keeping local development practical.
- Redis-backed idempotency, caching, and rate limiting.
- Kafka-backed durable business events and replayable consumers.
- RabbitMQ/JMS-style command processing for targeted partner callbacks.
- A small Go gRPC/protobuf risk scoring service.
- Production-grade testing, observability, and containerized local development.

Because this project is meant to signal enterprise readiness, dependency choices should favor stable, mainstream, actively supported versions over experimental or highly niche libraries.

## Decision
Use the following initial project stack and dependency baseline.

### Runtime and Build
- Java runtime: `Java 25 LTS`.
- Java build tool: `Maven 3.9.x`.
- Java framework: `Spring Boot 4.0.6`.
- Spring Framework: managed by Spring Boot, currently `Spring Framework 7.0.7+`.
- Go runtime: `Go 1.26.3`.
- Container runtime: Docker Compose for local development, with Kubernetes manifests or Helm added later.

### Java Service Stack
- Primary Java service style: Spring Boot WebFlux with Reactor.
- Package organization: by domain or feature, not by technical layer.
- Dependency injection: constructor injection with `private final` dependencies.
- Configuration: `application.yml`, profile-specific YAML files, and typed `@ConfigurationProperties`.
- Validation: Jakarta Bean Validation on request DTOs and command objects.
- Error handling: global WebFlux exception handling with stable API error contracts.
- Security: Spring Security with role-based access for `MERCHANT`, `OPS`, `AUDITOR`, `ADMIN`, and `SERVICE`.
- Observability: Spring Boot Actuator, Micrometer, Prometheus, Grafana, structured logs, and correlation IDs.

### Persistence and Data Access
- Production target: Oracle-compatible relational schema design.
- Local database: `PostgreSQL 18.4`.
- Migration tool: Flyway unless an implementation need emerges for Liquibase-specific behavior.
- Java database access:
  - Use R2DBC for reactive request-path persistence where non-blocking behavior matters.
  - Isolate any JDBC/JPA blocking integration behind bounded schedulers or worker services.
  - Do not mix blocking database calls into WebFlux event-loop code.
- Schema design: keep migrations portable across PostgreSQL and Oracle-compatible SQL where practical.

### Cache and Messaging
- Redis: `Redis 8.6.3`.
- Kafka broker line: `Apache Kafka 4.3.0` for local infrastructure.
- Kafka Java integration: Spring for Apache Kafka, with versions managed by the Spring Boot BOM unless an explicit compatibility issue requires an override.
- RabbitMQ: `RabbitMQ 4.3.0`.
- RabbitMQ/JMS integration: Spring AMQP for implementation, with JMS concepts documented around acknowledgement, retry, DLQ, and one-consumer command semantics.

### Go gRPC Risk Service
- Language/runtime: `Go 1.26.3`.
- RPC framework: gRPC with protobuf contracts.
- Service scope: deterministic risk scoring only, with rule versioning, reason codes, structured logs, health checks, graceful shutdown, and unit tests.
- Contract ownership: protobuf files live in a shared contract module or top-level `proto/` directory and generate Java and Go clients.

### Testing
- Unit tests: JUnit 5, Mockito, AssertJ for Java; Go standard `testing` package for Go.
- Reactive tests: Reactor Test.
- Spring test slices: `@WebFluxTest`, focused application tests, and integration tests as needed.
- Integration tests: Testcontainers for PostgreSQL, Redis, Kafka, RabbitMQ, and optionally the Go gRPC service.
- Contract tests: validate OpenAPI and protobuf compatibility before service integration changes are merged.

### Dependency Management Policy
- Use Spring Boot dependency management as the source of truth for Spring, Reactor, Micrometer, validation, logging, Jackson, Spring Security, Spring Kafka, and testing libraries.
- Avoid direct version overrides unless there is a documented compatibility, security, or feature reason.
- Pin infrastructure image versions in Docker Compose rather than using `latest`.
- Upgrade patch versions routinely, but require an ADR update for:
  - Java major or LTS runtime changes.
  - Spring Boot minor or major upgrades.
  - Database major version changes.
  - Kafka, RabbitMQ, Redis, or Go major version changes.
  - Switching from WebFlux to Spring MVC, R2DBC to JPA-first persistence, Kafka to another event platform, or RabbitMQ to another command broker.

## Consequences
This gives the project a modern and defensible stack that matches the portfolio goal: Java-first, reactive, fintech-oriented, observable, and testable. Java 25 LTS keeps the project current while avoiding the operational risk of building the core services on a non-LTS Java feature release.

Spring Boot 4 introduces a current Spring generation and aligns the project with Jakarta-era enterprise development. The tradeoff is that some third-party libraries may lag behind Spring Boot 4, so the project should lean on Spring-managed dependencies and avoid unnecessary niche dependencies early.

Using PostgreSQL locally while designing Oracle-compatible schemas keeps local development accessible without losing the enterprise database talking points. The tradeoff is that migrations and SQL features must be reviewed for portability instead of assuming every PostgreSQL feature is acceptable.

WebFlux and R2DBC make the payment API suitable for non-blocking orchestration, but they require discipline around event-loop safety. Any blocking integration must be explicit, isolated, measured, and documented.

Kafka and RabbitMQ both remain in scope because they model different enterprise messaging needs: Kafka for durable replayable facts, RabbitMQ for targeted command work. This adds local infrastructure complexity, but it strengthens the project as a realistic distributed-system portfolio piece.

## Rejected Alternatives
- Use Spring MVC as the primary web stack: rejected because the project explicitly targets reactive orchestration and WebFlux/Reactor expertise.
- Use Java 26 as the Java runtime: rejected because Java 25 is the current LTS baseline and is a better default for a fintech-style portfolio.
- Use only Kafka for all messaging: rejected because the project intentionally demonstrates the boundary between durable event streams and one-worker command queues.
- Use only RabbitMQ/JMS for all messaging: rejected because replayable audit, settlement projection, and operational event processing are a better fit for Kafka.
- Use Oracle Database only for local development: rejected because it raises local setup friction; Oracle compatibility will be handled through schema discipline and documentation.
- Use Gradle as the Java build tool: rejected for now because Maven is simple, common in enterprise Java shops, and adequate for this portfolio project.

## References
- Project brief: `docs/Project.md`
- Spring Boot system requirements: https://docs.spring.io/spring-boot/system-requirements.html
- Oracle Java SE support roadmap: https://www.oracle.com/java/technologies/java-se-support-roadmap.html
- Go release history: https://go.dev/doc/devel/release
- PostgreSQL latest releases: https://www.postgresql.org/
- Apache Kafka 4.3.0 release announcement: https://kafka.apache.org/blog/2026/05/22/apache-kafka-4.3.0-release-announcement/
- RabbitMQ release information: https://www.rabbitmq.com/release-information
- Redis release downloads: https://download.redis.io/releases/
