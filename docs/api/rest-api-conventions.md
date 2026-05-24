# REST API Conventions

## Scope

These conventions apply to public REST endpoints exposed by `payment-orchestrator-service`.

Phase 1 establishes the REST contract shape before domain endpoints are implemented. Payment authorization, lookup,
reversal, and operations endpoints will follow these conventions in later phases.

## Versioning

All REST API endpoints use a path version prefix:

```text
/api/v1
```

The Java service keeps the current prefix in `ApiPaths.API_V1`.

Examples:

```text
GET /api/v1/contract/ping
POST /api/v1/payments/authorize
GET /api/v1/payments/{paymentId}
POST /api/v1/payments/{paymentId}/reverse
```

Versioning is path-based because it is easy to inspect in logs, OpenAPI, client code, and gateway routing rules.

## Contract Ping

The first contract-only endpoint is:

```http
GET /api/v1/contract/ping
```

It returns:

```json
{
  "serviceName": "payment-orchestrator-service",
  "apiVersion": "v1",
  "correlationId": "corr_123"
}
```

This endpoint exists to verify the API version prefix, OpenAPI discovery, correlation ID behavior, and basic WebFlux
request handling before payment business endpoints exist.

## Correlation ID

Clients may pass:

```http
X-Correlation-Id: corr_123
```

If the header is missing or blank, the service generates one. The resolved value is returned in the `X-Correlation-Id`
response header and appears in error responses.

## Error Format

REST errors use `ApiErrorResponse`.

Validation failures, malformed requests, conflicts, not-found cases, downstream failures, authentication failures,
authorization failures, and fallback internal errors should all use the same top-level response shape.

## Payload Rules

- Use JSON request and response bodies.
- Use stable machine-readable IDs for domain resources.
- Represent money amounts as integer minor units.
- Do not expose sensitive customer, merchant, credential, token, or raw payment data in errors.
- Keep controllers thin; validation and request/response mapping belong at the API boundary, while business decisions
  belong in application/domain services.

## OpenAPI

OpenAPI JSON is exposed through SpringDoc at the standard SpringDoc endpoint.

Swagger UI is enabled for local development when provided by the SpringDoc WebFlux UI dependency.
