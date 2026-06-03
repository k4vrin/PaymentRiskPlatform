# Phase 4: Go Risk Scoring gRPC Service

## Purpose

Phase 4 turns the Go `risk-scoring-service` from a skeleton into the internal gRPC risk backend used by the Java payment
orchestrator.

```text
How does the platform score a payment authorization request with deterministic, explainable risk rules?
```

The Go service owns risk scoring. The Java payment orchestrator should keep calling the `RiskScoringClient` port and
should not learn risk-rule details. The protobuf contract remains the boundary between the services.

## Product Behavior We Are Building

Phase 4 implements this internal gRPC method:

```proto
rpc ScorePayment(ScorePaymentRequest) returns (ScorePaymentResponse);
```

Example request:

```json
{
  "paymentId": "pay_01HX7R0BYV9Y6CNW3HZ7R8E4P2",
  "amountMinor": 129900,
  "currency": "USD",
  "merchantId": "mer_01HX7Q9K2V6M8P4A3B9C1D2E3F",
  "customerId": "cus_01HX7QAF4CQ8YFZ3M9N2W1P0VK",
  "deviceFingerprint": "device_abc123",
  "correlationId": "corr_01HX7R2HBK51S6ZGJ7FN9K4M8D"
}
```

Example response:

```json
{
  "score": 65,
  "decision": "RISK_DECISION_REVIEW_REQUIRED",
  "reasonCodes": [
    "RISK_REASON_CODE_HIGH_AMOUNT",
    "RISK_REASON_CODE_MERCHANT_RISK_THRESHOLD_EXCEEDED"
  ],
  "ruleHits": [
    {
      "ruleId": "HIGH_AMOUNT_RULE",
      "reasonCode": "RISK_REASON_CODE_HIGH_AMOUNT",
      "scoreDelta": 35,
      "message": "Payment amount exceeds the high amount threshold."
    },
    {
      "ruleId": "MERCHANT_RISK_THRESHOLD_RULE",
      "reasonCode": "RISK_REASON_CODE_MERCHANT_RISK_THRESHOLD_EXCEEDED",
      "scoreDelta": 30,
      "message": "Merchant matches the local high-risk placeholder rule."
    }
  ],
  "ruleVersion": "local-v1"
}
```

## Important Rules

Scoring must be deterministic for fixed inputs and configuration. Phase 4 does not depend on PostgreSQL, Redis, Kafka,
RabbitMQ, or external risk providers.

The response must explain the score:

- `score` is the sum of matching rule score deltas;
- `decision` is derived from configured score thresholds;
- `reason_codes` is a stable summary of matched reasons;
- `rule_hits` contains detailed rule-level explanations;
- `rule_version` identifies the rule set used for the result.

The service must not log raw device fingerprints. If device information is needed in logs, log a redacted marker or a
hash added specifically for observability later.

## Target Flow

```text
Java payment-orchestrator-service
  -> GrpcRiskScoringClient builds ScorePaymentRequest
  -> Go risk-scoring-service receives ScorePayment
  -> gRPC handler validates required fields
  -> Handler maps protobuf request to internal risk request
  -> Scorer applies deterministic rules in stable order
  -> Decision policy maps score to APPROVED, REVIEW_REQUIRED, or DECLINED
  -> Handler maps internal result to ScorePaymentResponse
  -> Java risk adapter maps response back to application DTOs
```

## Layer-By-Layer Design

### Command Layer

Files should live under:

```text
services/risk-scoring-service/cmd/risk-scoring-service
```

`main.go` is the composition root. It should load config, configure logging, construct the scorer and gRPC server, start
serving, and coordinate graceful shutdown.

`main.go` should not contain scoring rules or protobuf mapping logic.

### Config Layer

Files should live under:

```text
services/risk-scoring-service/internal/config
```

The config layer should parse and validate environment values:

- service environment;
- host and gRPC port;
- rule version;
- approve and review score thresholds;
- log level;
- shutdown timeout.

Invalid configuration should fail startup instead of silently falling back to unsafe values.

### Risk Layer

Files should live under:

```text
services/risk-scoring-service/internal/risk
```

The risk layer owns scoring behavior and should use internal models. It should not import generated protobuf types.

Expected concepts:

- scoring request;
- scoring result;
- rule hit;
- risk rule interface or function shape;
- deterministic scorer;
- decision policy;
- rule version.

### gRPC Layer

Files should live under:

```text
services/risk-scoring-service/internal/grpc
```

The gRPC layer adapts generated protobuf requests to the internal scorer and maps scorer results back to protobuf
responses. It owns transport validation and gRPC status errors.

The gRPC layer should not contain risk-rule business logic.

### Health Layer

Files should live under:

```text
services/risk-scoring-service/internal/health
```

Phase 4 should use the standard gRPC health service. The service should report `SERVING` after startup and
`NOT_SERVING` during shutdown.

## Rule Design

Phase 4 uses local deterministic rules:

- high amount rule;
- suspicious currency rule;
- repeated device placeholder rule;
- merchant risk threshold placeholder rule;
- low-risk fallback when no positive-risk rules match.

The repeated-device and merchant-threshold rules are placeholders. They should be deterministic and documented, but
they should not pretend to use real historical state until later phases add storage or streaming inputs.

## Error Behavior

Invalid gRPC requests should return gRPC validation errors, not successful responses with unspecified enum values.

Important validation cases:

- missing `payment_id`;
- non-positive `amount_minor`;
- missing or malformed `currency`;
- missing `merchant_id`;
- missing `customer_id`.

Correlation ID is useful for tracing but should not be required for scoring correctness.

## Correlation ID Behavior

Phase 4 accepts correlation ID from `ScorePaymentRequest.correlation_id`. The Java side has a TODO for future metadata
propagation, but the message field is the current contract.

The Go service should include correlation ID in request-scoped logs when present.

## Step-By-Step Implementation Plan

### Step 1: Package And Boundary Structure

Keep the Go service organized around composition, config, risk, gRPC transport, and health. This prevents generated
protobuf code from leaking into rule logic.

### Step 2: Typed Configuration

Parse and validate environment variables from `.env.example`. Fail startup on invalid ports, thresholds, or shutdown
timeouts.

### Step 3: Structured Logging

Configure `log/slog` with service and environment attributes. Log lifecycle events and request summaries without raw
sensitive values.

### Step 4: Risk Domain Models

Create internal request/result/rule-hit models. These are the core types used by the scorer and tests.

### Step 5: Decision Policy

Map numeric scores to approved, review, and declined decisions using configured thresholds.

### Step 6: High Amount Rule

Add a deterministic high amount score contribution and reason code.

### Step 7: Suspicious Currency Rule

Normalize currency and add a score contribution for suspicious currencies.

### Step 8: Repeated Device Placeholder Rule

Add deterministic placeholder behavior without external state.

### Step 9: Merchant Risk Threshold Rule

Add deterministic placeholder behavior for merchants without external state.

### Step 10: Low-Risk Fallback

Return a low-risk reason and explanation when no positive-risk rules match.

### Step 11: Score Aggregation

Apply rules in stable order, sum score deltas, deduplicate reason codes, and return all rule hits.

### Step 12: Protobuf Mapping

Map between generated protobuf types and internal risk models in one place.

### Step 13: gRPC Risk Server

Register and implement `RiskScoringService.ScorePayment`.

### Step 14: Request Validation

Reject malformed requests with appropriate gRPC status errors before scoring.

### Step 15: Correlation ID Handling

Carry request correlation ID into logs and keep current message-field propagation explicit.

### Step 16: Server Startup

Start the gRPC server from `main.go` on configured host and port.

### Step 17: Health Service

Register the standard gRPC health service and update serving state during lifecycle transitions.

### Step 18: Graceful Shutdown

Handle interrupt signals, stop accepting new work, and force stop only after the configured timeout.

### Step 19: Go Integration Tests

Start the service on an ephemeral port and call it through a real gRPC client.

### Step 20: Java-To-Go Verification

Prove the Java gRPC adapter can call a running Go service without making the normal Java unit tests depend on a manual
process.

### Step 21: Runtime Documentation

Update startup commands, config examples, and scoring examples.

### Step 22: Final Verification

Run Go tests, protobuf contract tests, focused Java adapter tests, and local startup/shutdown checks.

## Testing Expectations

Use Go unit tests for:

- config defaults and validation;
- decision threshold boundaries;
- each individual risk rule;
- low-risk fallback;
- score aggregation;
- protobuf mapping.

Use Go gRPC tests for:

- low-risk `ScorePayment`;
- multi-rule `ScorePayment`;
- invalid request status errors;
- gRPC health service;
- generated protobuf contract construction.

Use Java tests for:

- Java gRPC adapter mapping of approved, review, and declined responses;
- deadline exceeded mapping to `RISK_SERVICE_TIMEOUT`;
- unavailable mapping to `DOWNSTREAM_UNAVAILABLE`;
- a focused Java-to-Go integration path where practical.

## Common Mistakes To Avoid

- Do not put risk rules in `main.go`.
- Do not let generated protobuf types leak into the core scoring rules.
- Do not return unspecified protobuf enum values for valid responses.
- Do not make scoring non-deterministic by using wall-clock time, random values, or external state in Phase 4.
- Do not log raw device fingerprints.
- Do not make the Java unit test suite require a manually started Go process.
- Do not silently accept invalid threshold configuration.
- Do not add real repeated-device persistence before the storage design is defined.

## Completion Criteria

Phase 4 is complete when:

- the Go service starts locally and registers `RiskScoringService`;
- `ScorePayment` returns deterministic scores, decisions, reason codes, rule hits, and rule version;
- invalid requests return appropriate gRPC status errors;
- gRPC health reports serving state;
- the service handles graceful shutdown;
- `go test ./...` passes in `services/risk-scoring-service`;
- Java risk adapter behavior remains covered;
- README and roadmap are updated with any added files and runtime commands.

## Related Documents

- PRD: `docs/Project.md`
- API roadmap: `docs/ApiRoadmap.md`
- Risk gRPC contract: `docs/api/risk-grpc-contract.md`
- Payment authorization workflow: `docs/phase-2-payment-authorization.md`
- Correlation ID contract: `docs/api/correlation-id.md`
