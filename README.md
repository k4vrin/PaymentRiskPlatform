# Reactive Payment Risk Platform

Java-first payment authorization and risk platform for demonstrating Spring Boot WebFlux, reactive persistence, Redis idempotency, Kafka event processing, RabbitMQ command work, Go gRPC integration, observability, and production-oriented testing.

## Repository Layout

```text
docs/
  Project.md
  ApiRoadmap.md
  adr/
platform/
  compose.local.yaml
  prometheus/
services/
  payment-orchestrator-service/
  risk-scoring-service/
scripts/
```

## Prerequisites

- Java 25
- Maven through the generated Spring Boot wrapper
- Go 1.26.3
- Docker and Docker Compose
- Protocol Buffers compiler, `protoc`
- Go protobuf plugins:
  - `protoc-gen-go`
  - `protoc-gen-go-grpc`

Install protobuf tooling on macOS with Homebrew:

```bash
brew install protobuf
```

Install the Go protobuf plugins:

```bash
go install google.golang.org/protobuf/cmd/protoc-gen-go@latest
go install google.golang.org/grpc/cmd/protoc-gen-go-grpc@latest
```

Make sure the Go install binary directory is on your shell path:

```bash
export PATH="$PATH:$(go env GOPATH)/bin"
```

Verify the tools:

```bash
protoc --version
protoc-gen-go --version
protoc-gen-go-grpc --version
```

## Local Infrastructure

Start the local platform:

```bash
make platform-up
```

Check containers:

```bash
make platform-ps
```

Stop the local platform:

```bash
make platform-down
```

Local service ports:

| Service | Port | Notes |
| --- | ---: | --- |
| PostgreSQL | `5432` | Database `payment_risk`, user `payment_risk` |
| Redis | `6379` | Cache, idempotency, rate limits |
| Kafka | `9092` | Durable business events |
| RabbitMQ | `5672` | Partner callback commands |
| RabbitMQ Management | `15672` | User `payment_risk`, password `payment_risk` |
| Prometheus | `9090` | Metrics scraping |
| Grafana | `3000` | User `admin`, password `admin` |

## Development Commands

Validate the Spring Boot service:

```bash
make java-validate
```

Run Java tests:

```bash
make java-test
```

Run Go tests:

```bash
make go-test
```

Run all available checks:

```bash
make test
```

Regenerate contracts and run contract checks:

```bash
make contract-test
```

Start the Spring Boot service:

```bash
make spring-run
```

`make java-run` is also available as an alias for `make spring-run`.

Start the Go risk scoring service:

```bash
make risk-run
```

## Current Implementation Status

The project has completed the foundation and API contract baseline work and is currently in Phase 2: Payment
Authorization API.

Completed foundations include:

- Spring Boot WebFlux payment orchestrator service structure.
- Go risk scoring service skeleton.
- Local Docker Compose platform for PostgreSQL, Redis, Kafka, RabbitMQ, Prometheus, and Grafana.
- REST API conventions, API versioning, OpenAPI setup, correlation ID handling, and global error responses.
- Shared protobuf contract for the risk scoring gRPC API.
- Generated Go protobuf and gRPC files.
- Payment authorization API shell at `POST /api/v1/payments/authorize`.
- Payment domain model, value objects, lifecycle states, and state-transition validation.
- Flyway migration for payments, authorizations, risk decisions, idempotency records, and outbox events.
- Reactive entity models, repositories, and persistence mappers.
- Database idempotency read path for replaying completed stored authorization responses.
- In-memory idempotency handling for the current authorization workflow.
- Tests for API contracts, correlation IDs, error handling, domain value objects, persistence mappers, and the current
  authorization behavior.

The current authorization endpoint is still a contract-only workflow. It validates the request, creates a payment
aggregate in memory, applies a fake approved risk decision, returns an authorized response, and supports duplicate
request replay through the in-memory idempotency store.

Main work not implemented yet:

- Database idempotency write path for creating and completing durable records during authorization.
- Redis response snapshot cache with database fallback.
- Durable payment, authorization, and risk decision writes inside the authorization workflow.
- Java gRPC risk client and real Go risk scoring server implementation.
- Risk decision mapping for approved, declined, review-required, timeout, and unavailable outcomes.
- Outbox event payloads, mappers, and writes during authorization.
- Reactive transaction boundary across payment persistence, idempotency completion, and outbox insertion.
- Payment lookup and reversal APIs.
- Kafka outbox relay, audit consumer, settlement projection consumer, and RabbitMQ callback worker.
- Operations APIs, security roles/authentication, observability dashboards, CI, and release-readiness work.

See `docs/ApiRoadmap.md` and `docs/phase-2-payment-authorization.md` for the detailed tracker.
