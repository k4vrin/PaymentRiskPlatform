# API Error Contract

## Scope

The Java payment orchestrator returns a stable error response for REST failures.

The response type is `ApiErrorResponse`.

## Shape

```json
{
  "timestamp": "2026-05-24T12:00:00Z",
  "status": 400,
  "code": "VALIDATION_FAILED",
  "message": "Request validation failed.",
  "path": "/api/v1/payments/authorize",
  "correlationId": "corr_123",
  "fieldErrors": [
    {
      "field": "amountMinor",
      "message": "must be greater than 0"
    }
  ]
}
```

## Fields

- `timestamp`: time the error response was created.
- `status`: HTTP status code.
- `code`: stable machine-readable `ApiErrorCode`.
- `message`: safe human-readable summary.
- `path`: request path that failed.
- `correlationId`: resolved request correlation ID.
- `fieldErrors`: validation field errors, empty when the error is not field-specific.

## Error Code Groups

`ApiErrorCode` is a sealed interface with grouped enum implementations:

- `Business`;
- `Security`;
- `Validation`;
- `Infrastructure`.

This keeps the JSON code stable while making the Java type system show the category of the failure.

## Mapping Rules

- Bean Validation and binding failures return `400` with `VALIDATION_FAILED`.
- Malformed request input returns `400` with `MALFORMED_REQUEST`.
- Not-found business failures return `404` with `RESOURCE_NOT_FOUND`.
- Business conflicts return `409` with a specific business error code.
- Downstream timeouts return `504` with `RISK_SERVICE_TIMEOUT`.
- Downstream unavailability returns `503` with `DOWNSTREAM_UNAVAILABLE`.
- Authentication failures return `401`.
- Authorization failures return `403`.
- Unexpected failures return `500` with `INTERNAL_ERROR`.

## Safety

Error responses must not expose:

- credentials;
- access tokens;
- raw payment data;
- sensitive customer data;
- internal stack traces;
- unsafe rejected values from validation failures.

Detailed diagnostic data belongs in structured logs tied to the same correlation ID, not in the public API response.
