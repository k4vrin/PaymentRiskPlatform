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
  grafana/
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

Validate Docker Compose config:

```bash
make compose-config
```

Build service images:

```bash
make image-build
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

The Go service listens on `RISK_SERVICE_HOST:RISK_SERVICE_GRPC_PORT`, defaulting to `0.0.0.0:9091`. The Spring Boot
local profile points its risk gRPC client at `localhost:9091`, so starting `make risk-run` before the Java service wires
the authorization flow to the local Go scorer.

Useful Go service configuration:

```text
RISK_SERVICE_ENV=local
RISK_SERVICE_HOST=0.0.0.0
RISK_SERVICE_NAME=risk-scoring-service
RISK_SERVICE_GRPC_PORT=9091
RISK_RULE_VERSION=local-v1
RISK_APPROVE_MAX_SCORE=49
RISK_REVIEW_MAX_SCORE=79
LOG_LEVEL=info
SHUTDOWN_TIMEOUT_SECONDS=10
```

Phase 4 risk rules are deterministic and infrastructure-free:

- high amount: amount above `10_000_000` minor units adds `HIGH_AMOUNT`;
- suspicious currency: `BTC`, `ETH`, `XTS`, or `XXX` adds `SUSPICIOUS_CURRENCY`;
- repeated device placeholder: device fingerprints prefixed with `repeat_` add `REPEATED_DEVICE`;
- merchant risk placeholder: merchant IDs prefixed with `high_risk_` add
  `MERCHANT_RISK_THRESHOLD_EXCEEDED`;
- low-risk fallback: clean requests receive `LOW_RISK_PAYMENT`.

## Current Implementation Status

The project has completed the foundation, API contract baseline, Phase 2 Payment Authorization API work, Phase 3 Payment
Lookup/Reversal work, Phase 4 Go Risk Scoring gRPC Service work, Phase 5 Operations API work, Phase 6 messaging work,
and Phase 7 security/observability/release-readiness work.

Completed foundations include:

- Spring Boot WebFlux payment orchestrator service structure.
- Go risk scoring service with deterministic scoring rules, gRPC health, structured logging, and graceful shutdown.
- Local Docker Compose platform for PostgreSQL, Redis, Kafka, RabbitMQ, Prometheus, and Grafana with provisioned
  dashboards.
- REST API conventions, API versioning, OpenAPI setup, correlation ID handling, and global error responses.
- Shared protobuf contract for the risk scoring gRPC API.
- Generated Go protobuf and gRPC files.
- Payment authorization API shell at `POST /api/v1/payments/authorize`.
- Payment domain model, value objects, lifecycle states, and state-transition validation.
- Flyway migration for payments, authorizations, risk decisions, idempotency records, and outbox events.
- Reactive entity models, repositories, and persistence mappers.
- Database idempotency read/write path for replaying completed stored authorization responses.
- Authorization workflow wiring for database idempotency records and response snapshots.
- Redis completed-response snapshot cache for authorization idempotency replay, with database fallback.
- Sensitive payment data hashing helpers for payment method tokens and device fingerprints.
- Durable payment, authorization, and risk decision writes during authorization.
- Kafka-ready outbox payload records, event-envelope mapping, and pending outbox writes for authorization outcomes.
- Reactive transaction boundary across payment state persistence, outbox insertion, and idempotency completion.
- Java gRPC risk client adapter with configurable host, port, timeout, response mapping, and timeout/unavailable
  fallback outcomes, verified against the Go protobuf contract.
- Risk decision mapping for approved, declined, review-required, timeout, and unavailable outcomes.
- Redis repopulation from durable database snapshots after cache misses.
- Merchant API key authentication, JWT-based ops/internal auth, secure headers, CORS, rate limiting, masking, and
  endpoint authorization tests.
- Micrometer metrics for API latency, authorization outcomes, risk latency/failures, Redis idempotency fallback, outbox
  publishing, consumer processing, dead letters, replay requests, and partner callbacks.
- CI workflow definitions for Java, Go, protobuf contracts, Docker Compose validation, and container image builds.
- Dockerfiles for the Java payment orchestrator and Go risk scoring service.
- Tests for API contracts, correlation IDs, error handling, domain value objects, persistence mappers, Redis cache
  behavior, transaction rollback, duplicate idempotency replay, durable authorization workflow, security controls, and
  observability helpers.

The current authorization endpoint validates the request, creates a payment aggregate, calls the risk scoring client,
maps the risk result to an authorization or decline, persists the payment state and outbox events transactionally,
returns the authorization response, and supports duplicate request replay through Redis with the database idempotency
store as the durable source of truth.

Phase 5 operations APIs are available under `/api/v1/ops/**` for payment search, outbox inspection, dead-letter
inspection, replay requests, and consumer-lag visibility. Ops endpoints accept JWT bearer tokens with `OPS` or `ADMIN`;
local/test fallback headers are retained for focused tests.

Phase 6 messaging includes Kafka outbox publishing, audit and settlement consumers, ops metrics projection, dead-letter
persistence, and RabbitMQ partner callback command handling.

Phase 7 readiness artifacts:

- `docs/api/endpoint-authorization-matrix.md`
- `docs/api/observability-metrics.md`
- `docs/testing/end-to-end-platform-self-test.md`
- `docs/runbooks/linux-operations-runbook.md`
- `docs/incidents/failed-risk-service.md`
- `docs/release/phase-7-release-readiness-checklist.md`
- `platform/grafana/dashboards/`
- `.github/workflows/`

See `docs/ApiRoadmap.md` and `docs/phase-2-payment-authorization.md` for the detailed tracker.
Phase 3 planning is documented in `docs/phase-3-payment-lookup-and-reversal.md`.
Phase 4 planning is documented in `docs/phase-4-go-risk-scoring-grpc-service.md`.
Phase 5 planning is documented in `docs/phase-5-operations-api.md`.
Phase 6 messaging is documented in `docs/phase-6-messaging-and-event-apis.md`.
