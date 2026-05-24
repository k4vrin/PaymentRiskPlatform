# Correlation ID Convention

The payment risk platform uses a correlation ID to connect all work caused by the same external request or internal workflow. It is not an authentication token and must not contain sensitive data.

## Header

REST clients pass the correlation ID with:

```http
X-Correlation-Id: corr_01HYX8J20EXAMPLE
```

If the client does not provide `X-Correlation-Id`, the payment orchestrator generates one.

## Behavior

- Incoming REST requests may provide `X-Correlation-Id`.
- Blank or missing values are replaced with a generated UUID.
- The resolved correlation ID is added to the request exchange attributes as `correlationId`.
- The resolved correlation ID is returned on every REST response as `X-Correlation-Id`.
- API error responses include the same value in `ApiErrorResponse.correlationId`.

## Use In This App

The correlation ID is the tracing key for a payment authorization flow across the Java payment orchestrator and the Go risk scoring service.

It will be used to:

- connect REST access logs, application logs, and error responses for one request;
- trace a payment request through idempotency, risk scoring, persistence, and outbox publishing;
- propagate request identity into gRPC metadata when the Java service calls the Go risk service;
- propagate request identity into Kafka and RabbitMQ headers for asynchronous workflows;
- support debugging without exposing payment data, customer data, credentials, or tokens.

## Current Implementation

The Spring WebFlux service resolves correlation IDs in `CorrelationIdWebFilter`.

The current completed REST behavior is:

- preserve inbound `X-Correlation-Id`;
- generate a UUID when the header is missing or blank;
- add `X-Correlation-Id` to the response;
- make the value available to global API error responses.

Later phases will propagate the same value into:

- gRPC metadata;
- Kafka message headers;
- RabbitMQ message headers;
- structured logs and metrics labels where appropriate.
