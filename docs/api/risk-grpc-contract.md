# Risk gRPC Contract

## Scope

The risk gRPC contract defines how `payment-orchestrator-service` calls the Go `risk-scoring-service`.

The source of truth is:

```text
proto/risk/v1/risk_scoring.proto
```

Generated Java and Go code must not be edited manually.

## Service

```proto
service RiskScoringService {
  rpc ScorePayment(ScorePaymentRequest) returns (ScorePaymentResponse);
}
```

`ScorePayment` is unary. It is called on the synchronous payment authorization path, so callers must use a short
explicit timeout budget.

## Request

`ScorePaymentRequest` contains:

- `payment_id`: payment identifier from the Java orchestrator;
- `amount_minor`: amount in integer minor currency units;
- `currency`: ISO 4217 currency code;
- `merchant_id`: merchant identifier;
- `customer_id`: customer identifier;
- `device_fingerprint`: stable device signal used by risk rules;
- `correlation_id`: tracing value propagated from the REST request.

## Response

`ScorePaymentResponse` contains:

- `score`: final numeric risk score;
- `decision`: stable `RiskDecision` enum;
- `reason_codes`: stable machine-readable summary codes;
- `rule_hits`: detailed rule-level explanations;
- `rule_version`: version of the rule set used to score the request.

## Enum Rules

All protobuf enums reserve numeric value `0` for unspecified/default states.

Stable numeric values must not be reused after they are published. If a value is removed in a later version, the number
should be reserved in the proto file.

## Generation

Use:

```bash
make proto
```

Java generation is also performed by Maven during Java compilation. Go generation writes files under:

```text
proto/gen/go/risk/v1
```

Contract tests in both services construct generated `ScorePaymentRequest` and `ScorePaymentResponse` messages to catch
broken generated-code wiring.
