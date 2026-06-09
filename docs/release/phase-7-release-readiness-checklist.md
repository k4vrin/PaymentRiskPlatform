# Phase 7 Release Readiness Checklist

## Tests

- Java focused security/observability tests pass.
- Go tests pass with `make go-test`.
- Protobuf generation passes with `make proto`.
- Docker Compose config validates with `make compose-config`.
- Container images build with `make image-build`.

## Documentation

- Endpoint authorization matrix is current.
- Error contract documents masking and structured errors.
- Observability metrics doc lists API, authorization, risk, Redis, and messaging metrics.
- Linux operations runbook is available.
- Incident write-up is available.

## Dashboards

- Prometheus scrapes Spring Boot actuator metrics.
- Grafana datasource is provisioned.
- API health dashboard exists.
- Authorization dashboard exists.
- Risk service dashboard exists.
- Redis/idempotency dashboard exists.
- Kafka/outbox dashboard exists.
- Database health dashboard exists.

## Secrets

- No production secrets are committed.
- Local defaults are development-only.
- API key hashing pepper, JWT secret, and payment sensitive-data hash key are environment-backed.
- `.env` remains untracked.

## Local Environment

- `make platform-up` starts PostgreSQL, Redis, Kafka, RabbitMQ, Prometheus, and Grafana.
- `make risk-run` starts the Go risk scoring gRPC service.
- `make spring-run` starts the Spring Boot payment orchestrator.
- Prometheus is reachable at `http://localhost:9090`.
- Grafana is reachable at `http://localhost:3000`.
