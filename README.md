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

Start the Spring Boot service:

```bash
make spring-run
```

Start the Go risk scoring service:

```bash
make risk-run
```

## Current Implementation Status

The repository currently contains the generated Spring Boot service, the Go risk service skeleton, the first stack ADR, and the API roadmap. The next implementation step is the API contract baseline: shared protobuf files, generated Java and Go stubs, and the first REST error/correlation contract.
