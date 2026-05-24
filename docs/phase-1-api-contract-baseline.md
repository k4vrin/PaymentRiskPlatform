# Phase 1: API Contract Baseline

## Purpose

Phase 1 builds the contract foundation for the Reactive Payment Risk Platform before payment business workflows are implemented.

The goal is to make the service boundaries, request conventions, error format, correlation behavior, and cross-service contracts explicit early. This keeps later payment, risk, audit, outbox, and operations work from growing around unstable API shapes.

## What We Are Building

Phase 1 defines four contract surfaces:

- the external REST API baseline for the Java payment orchestrator;
- the internal gRPC contract between the Java service and the Go risk scoring service;
- the shared event envelope for Kafka and future asynchronous workflows;
- the shared API behavior for errors, validation, versioning, and correlation IDs.

This phase is intentionally contract-first. It does not implement payment authorization, persistence, idempotency, or risk scoring business rules yet.

## REST API Baseline

The Java service exposes REST endpoints through Spring Boot WebFlux.

In this phase, REST work focuses on conventions rather than domain behavior:

- use `/api/v1` as the versioned API prefix;
- expose OpenAPI JSON and Swagger UI for local contract inspection;
- add a lightweight contract endpoint such as `GET /api/v1/contract/ping`;
- return predictable response shapes;
- make correlation ID behavior visible at the API boundary.

The contract endpoint exists to prove the REST stack, OpenAPI setup, correlation ID filter, and error handling before the real payment endpoints are added.

## gRPC Risk Contract

The Go risk scoring service is called through gRPC using protobuf contracts.

Phase 1 defines `proto/risk/v1/risk_scoring.proto` as the source of truth for:

- `RiskScoringService`;
- unary `ScorePayment` RPC;
- `ScorePaymentRequest`;
- `ScorePaymentResponse`;
- risk decisions;
- reason codes;
- rule hits;
- correlation ID propagation fields;
- integer minor-unit amount representation.

The protobuf contract generates both Go and Java code. This ensures the Java orchestrator and Go risk service share the same typed contract instead of duplicating request and response models manually.

## Event Envelope

Kafka events and future asynchronous messages need a stable envelope before domain events are implemented.

Phase 1 documents the common event metadata:

- `schemaVersion`;
- `eventId`;
- `eventType`;
- `aggregateId`;
- `aggregateType`;
- `occurredAt`;
- `producer`;
- `correlationId`.

The envelope gives later outbox, audit, settlement, replay, and dead-letter work a consistent structure.

## Error Contract

Phase 1 creates a shared API error model for all REST failures.

The standard error response is:

```java
ApiErrorResponse(
    timestamp,
    status,
    code,
    message,
    path,
    correlationId,
    fieldErrors
)
```

Error codes are modeled with a sealed `ApiErrorCode` interface and grouped by category:

- `Business`;
- `Security`;
- `Validation`;
- `Infrastructure`.

The global WebFlux exception handler maps validation failures, malformed input, business conflicts, not-found cases, downstream failures, authentication failures, authorization failures, and unexpected errors into this response shape.

## Correlation ID Support

Every request gets a correlation ID.

The Java service accepts `X-Correlation-Id` from clients. If the header is missing or blank, the service generates one. The resolved value is:

- stored in the WebFlux exchange attributes;
- returned in the `X-Correlation-Id` response header;
- included in `ApiErrorResponse.correlationId`;
- reserved for later propagation into gRPC metadata, Kafka headers, RabbitMQ headers, and structured logs.

This lets one payment workflow be traced across REST, service calls, event publishing, and asynchronous workers.

## Developer Workflow

Phase 1 also makes contract generation repeatable:

- `make proto` generates protobuf code;
- Go generated protobuf code must compile with `go test ./...`;
- Java generated protobuf code must compile with Maven tests;
- OpenAPI endpoints must be available locally;
- the root README documents required protobuf tooling.

## Out Of Scope

Phase 1 does not build:

- payment authorization business logic;
- payment lifecycle state transitions;
- database schema or persistence logic;
- Redis idempotency behavior;
- real risk scoring rules;
- Kafka producers or consumers;
- RabbitMQ callback workers;
- authentication and role-based authorization.

Those belong to later phases after the API and messaging contracts are stable.

## Completion Criteria

Phase 1 is complete when:

- the protobuf risk contract is the single source of truth;
- Java and Go protobuf generation works from project commands;
- the Java service exposes OpenAPI for the REST contract;
- `/api/v1` versioning is documented and used by the first contract endpoint;
- validation and error responses use `ApiErrorResponse`;
- missing correlation IDs are generated;
- inbound correlation IDs are preserved;
- REST, gRPC, event envelope, error, and correlation conventions are documented;
- Java and Go tests pass after contract generation.

## Current Status

Completed:

- shared protobuf folder and `risk_scoring.proto`;
- Go and Java protobuf generation;
- Spring OpenAPI setup;
- REST API versioning documentation;
- event envelope documentation;
- shared API package structure;
- global API error response model;
- stable grouped API error codes;
- global WebFlux exception handling;
- correlation ID support;
- first contract-only REST endpoint;
- Spring API tests for the contract endpoint;
- protobuf construction tests;
- REST, gRPC, error, and correlation contract documentation;
- developer command cleanup for contract testing.

Remaining:

- None for Phase 1.
