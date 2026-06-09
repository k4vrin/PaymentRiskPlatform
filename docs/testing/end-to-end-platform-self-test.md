# End-to-End Platform Self-Test

Use this guide to test the whole platform locally: infrastructure, Go risk scoring, Spring Boot payment orchestration,
merchant authentication, payment authorization, idempotency, lookup, reversal, ops APIs, outbox data, metrics, dashboards,
and automated checks.

## Prerequisites

- Docker Desktop or Docker Engine is running.
- Java 25 is installed.
- Go 1.26.3 is installed.
- `protoc`, `protoc-gen-go`, and `protoc-gen-go-grpc` are installed and on `PATH`.
- `curl`, `jq`, and `python3` are available.

Optional tools:

- `psql` if you prefer connecting directly instead of `docker exec`.
- `grpcurl` if you want to probe the Go gRPC service directly.

## 1. Start Local Infrastructure

From the repository root:

```bash
make platform-up
make platform-ps
```

Expected containers:

- `payment-risk-postgres`
- `payment-risk-redis`
- `payment-risk-kafka`
- `payment-risk-rabbitmq`
- `payment-risk-prometheus`
- `payment-risk-grafana`

If a container is unhealthy or stopped:

```bash
make platform-logs
```

## 2. Start Both Application Services

Use separate terminals.

Terminal 1:

```bash
make risk-run
```

The Go risk scoring service listens on `localhost:9091`.

Terminal 2:

```bash
make spring-run
```

The Spring Boot payment orchestrator listens on `localhost:8080` with the `local` profile. Flyway migrations run during
startup.

## 3. Check Basic Health

```bash
curl -sS http://localhost:8080/api/v1/contract/ping | jq .
curl -sS http://localhost:8080/actuator/health | jq .
curl -sS http://localhost:8080/actuator/prometheus | head
```

Expected result:

- contract ping returns a JSON response;
- actuator health is `UP`;
- Prometheus metrics are emitted as text.

## 4. Create a Local Merchant API Key

Payment APIs require `X-API-Key: <keyId>.<secret>`. The local database creates the API key table, but it does not store a
plaintext demo secret. Insert a local-only test key after the Spring service has started and migrations have run.

```bash
export API_KEY_ID="mk_local_self_test"
export API_KEY_SECRET="sk_local_self_test_secret"
export API_KEY="$API_KEY_ID.$API_KEY_SECRET"

export API_KEY_HASH="$(python3 -c 'import hashlib,hmac,os
pepper=os.environ.get("PAYMENT_RISK_API_KEY_HASHING_PEPPER","local-development-api-key-hashing-pepper")
key=os.environ["API_KEY_ID"]
secret=os.environ["API_KEY_SECRET"]
print("hmac-sha256:"+hmac.new(pepper.encode(), f"{key}:{secret}".encode(), hashlib.sha256).hexdigest())')"

docker exec payment-risk-postgres psql -U payment_risk -d payment_risk -c "
insert into merchant_api_keys (key_id, secret_hash, merchant_id, status)
values ('$API_KEY_ID', '$API_KEY_HASH', 'mer_local_self_test', 'ACTIVE')
on conflict (key_id) do update
set secret_hash = excluded.secret_hash,
    merchant_id = excluded.merchant_id,
    status = excluded.status;
"
```

Do not use this key outside local testing.

## 5. Authorize a Low-Risk Payment

```bash
curl -sS -X POST http://localhost:8080/api/v1/payments/authorize \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $API_KEY" \
  -H "X-Correlation-Id: corr-self-test-auth-001" \
  -d '{
    "merchantId": "mer_local_self_test",
    "customerId": "cus_self_test_001",
    "amountMinor": 1299,
    "currency": "USD",
    "paymentMethodToken": "pmt_tok_self_test_001",
    "deviceFingerprint": "dfp_self_test_001",
    "externalReference": "order_self_test_001",
    "idempotencyKey": "idem-self-test-auth-001"
  }' | tee /tmp/payment-authorize.json | jq .
```

Expected result:

- `status` is usually `AUTHORIZED`;
- `riskDecision` is usually `APPROVED`;
- `reasonCodes` includes `LOW_RISK_PAYMENT`;
- `paymentId` is present.

Save the ID:

```bash
export PAYMENT_ID="$(jq -r '.paymentId' /tmp/payment-authorize.json)"
echo "$PAYMENT_ID"
```

## 6. Verify Authorization Idempotency

Run the same request again with the same body and same `idempotencyKey`:

```bash
curl -sS -X POST http://localhost:8080/api/v1/payments/authorize \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $API_KEY" \
  -H "X-Correlation-Id: corr-self-test-auth-001-replay" \
  -d '{
    "merchantId": "mer_local_self_test",
    "customerId": "cus_self_test_001",
    "amountMinor": 1299,
    "currency": "USD",
    "paymentMethodToken": "pmt_tok_self_test_001",
    "deviceFingerprint": "dfp_self_test_001",
    "externalReference": "order_self_test_001",
    "idempotencyKey": "idem-self-test-auth-001"
  }' | jq .
```

Expected result:

- the response replays the original completed authorization;
- the `paymentId` matches `$PAYMENT_ID`;
- no duplicate payment should be created for the same idempotent request.

To test conflict handling, reuse the same `idempotencyKey` with a changed request body:

```bash
curl -i -sS -X POST http://localhost:8080/api/v1/payments/authorize \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $API_KEY" \
  -d '{
    "merchantId": "mer_local_self_test",
    "customerId": "cus_self_test_001",
    "amountMinor": 9999,
    "currency": "USD",
    "paymentMethodToken": "pmt_tok_self_test_001",
    "deviceFingerprint": "dfp_self_test_001",
    "externalReference": "order_self_test_001",
    "idempotencyKey": "idem-self-test-auth-001"
  }'
```

Expected result: `409 Conflict`.

## 7. Look Up the Payment

```bash
curl -sS "http://localhost:8080/api/v1/payments/$PAYMENT_ID" \
  -H "X-API-Key: $API_KEY" | jq .
```

Expected result:

- payment fields match the authorization request;
- raw payment method token and raw device fingerprint are not returned;
- authorization and risk details are present.

## 8. Reverse the Payment

```bash
curl -sS -X POST "http://localhost:8080/api/v1/payments/$PAYMENT_ID/reverse" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $API_KEY" \
  -H "X-Correlation-Id: corr-self-test-reversal-001" \
  -d '{
    "idempotencyKey": "idem-self-test-reversal-001",
    "reason": "self_test"
  }' | tee /tmp/payment-reversal.json | jq .
```

Expected result:

- `paymentId` matches `$PAYMENT_ID`;
- `status` indicates the payment is reversed;
- `reversalId` is present.

Run the same reversal request again to verify reversal idempotency:

```bash
curl -sS -X POST "http://localhost:8080/api/v1/payments/$PAYMENT_ID/reverse" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $API_KEY" \
  -d '{
    "idempotencyKey": "idem-self-test-reversal-001",
    "reason": "self_test"
  }' | jq .
```

Expected result: the completed reversal is replayed.

## 9. Exercise Risk Rule Variants

High amount rule:

```bash
curl -sS -X POST http://localhost:8080/api/v1/payments/authorize \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $API_KEY" \
  -d '{
    "merchantId": "mer_local_self_test",
    "customerId": "cus_self_test_high_amount",
    "amountMinor": 10000001,
    "currency": "USD",
    "paymentMethodToken": "pmt_tok_self_test_high_amount",
    "deviceFingerprint": "dfp_self_test_high_amount",
    "externalReference": "order_self_test_high_amount",
    "idempotencyKey": "idem-self-test-high-amount-001"
  }' | jq .
```

Expected result: `reasonCodes` includes `HIGH_AMOUNT`.

Suspicious currency rule:

```bash
curl -sS -X POST http://localhost:8080/api/v1/payments/authorize \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $API_KEY" \
  -d '{
    "merchantId": "mer_local_self_test",
    "customerId": "cus_self_test_currency",
    "amountMinor": 1299,
    "currency": "XXX",
    "paymentMethodToken": "pmt_tok_self_test_currency",
    "deviceFingerprint": "dfp_self_test_currency",
    "externalReference": "order_self_test_currency",
    "idempotencyKey": "idem-self-test-currency-001"
  }' | jq .
```

Expected result: `reasonCodes` includes `SUSPICIOUS_CURRENCY`.

Repeated device rule:

```bash
curl -sS -X POST http://localhost:8080/api/v1/payments/authorize \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $API_KEY" \
  -d '{
    "merchantId": "mer_local_self_test",
    "customerId": "cus_self_test_repeat",
    "amountMinor": 1299,
    "currency": "USD",
    "paymentMethodToken": "pmt_tok_self_test_repeat",
    "deviceFingerprint": "repeat_self_test_device",
    "externalReference": "order_self_test_repeat",
    "idempotencyKey": "idem-self-test-repeat-001"
  }' | jq .
```

Expected result: `reasonCodes` includes `REPEATED_DEVICE`.

## 10. Inspect Operations APIs

Local ops endpoints can use fallback headers in the `local` profile.

Payment search:

```bash
curl -sS "http://localhost:8080/api/v1/ops/payments?merchantId=mer_local_self_test&size=20" \
  -H "X-User-Id: local-ops" \
  -H "X-User-Roles: OPS" | jq .
```

Outbox inspection:

```bash
curl -sS "http://localhost:8080/api/v1/ops/outbox?size=20" \
  -H "X-User-Id: local-ops" \
  -H "X-User-Roles: OPS" | jq .
```

Dead-letter inspection:

```bash
curl -sS "http://localhost:8080/api/v1/ops/dead-letters?size=20" \
  -H "X-User-Id: local-ops" \
  -H "X-User-Roles: OPS" | jq .
```

Consumer lag:

```bash
curl -sS "http://localhost:8080/api/v1/ops/consumer-lag" \
  -H "X-User-Id: local-ops" \
  -H "X-User-Roles: OPS" | jq .
```

Replay request example for an outbox event:

```bash
export OUTBOX_EVENT_ID="$(docker exec payment-risk-postgres psql -U payment_risk -d payment_risk -tAc \
  "select event_id from outbox_events order by created_at desc limit 1")"

curl -sS -X POST "http://localhost:8080/api/v1/ops/replay/OUTBOX/$OUTBOX_EVENT_ID" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: local-ops" \
  -H "X-User-Roles: OPS" \
  -d '{"reason":"self_test"}' | jq .
```

Expected result: a replay job response with the target ID and request status.

## 11. Inspect Database State

Payments:

```bash
docker exec payment-risk-postgres psql -U payment_risk -d payment_risk -c "
select payment_id, merchant_id, customer_id, amount_minor, currency, status, created_at
from payments
order by created_at desc
limit 10;
"
```

Risk decisions:

```bash
docker exec payment-risk-postgres psql -U payment_risk -d payment_risk -c "
select payment_id, decision, score, reason_codes_json, rule_version, decided_at
from payment_risk_decisions
order by decided_at desc
limit 10;
"
```

Outbox events:

```bash
docker exec payment-risk-postgres psql -U payment_risk -d payment_risk -c "
select event_type, status, count(*)
from outbox_events
group by event_type, status
order by event_type, status;
"
```

Idempotency records:

```bash
docker exec payment-risk-postgres psql -U payment_risk -d payment_risk -c "
select scope, status, count(*)
from idempotency_records
group by scope, status
order by scope, status;
"
```

## 12. Check Messaging Infrastructure

Kafka topics:

```bash
docker exec payment-risk-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --list
```

RabbitMQ management UI:

- URL: `http://localhost:15672`
- User: `payment_risk`
- Password: `payment_risk`

Use the UI to inspect queues, exchanges, and connections after running payment flows.

## 13. Check Observability

Prometheus:

- URL: `http://localhost:9090`
- Target status: `Status > Target health`
- Java target should scrape `host.docker.internal:8080` at `/actuator/prometheus`.

Useful local checks:

```bash
curl -sS http://localhost:8080/actuator/prometheus | grep -E "paymentrisk|http_server|jvm" | head
```

Grafana:

- URL: `http://localhost:3000`
- User: `admin`
- Password: `admin`

Open the provisioned dashboards and confirm panels receive data after you run payment and ops API traffic.

## 14. Run Automated Checks

Run all contract and service tests:

```bash
make test
```

Run checks independently:

```bash
make proto
make java-test
make go-test
make compose-config
make image-build
```

Expected result: every command exits successfully.

## 15. Stop or Reset the Platform

Stop containers while keeping local volumes:

```bash
make platform-down
```

Delete local volumes and reset the database, Redis, Kafka, RabbitMQ, Prometheus, and Grafana state:

```bash
docker compose -f platform/compose.local.yaml down -v
```

Use the volume reset only when you want a clean local environment.

## Troubleshooting

`401 Unauthorized` on payment APIs:

- Confirm `X-API-Key` is set to `$API_KEY`.
- Confirm the API key row exists in `merchant_api_keys`.
- Re-run the local API key insert step after resetting volumes.

`403 Forbidden` on ops APIs:

- Include `X-User-Roles: OPS` or `X-User-Roles: ADMIN`.
- Include `X-User-Id` to make replay audit fields easy to read.

Java service cannot reach risk scoring:

- Start `make risk-run` before or alongside `make spring-run`.
- Confirm no other process is using port `9091`.

Prometheus target is down:

- Confirm the Java service is running on `localhost:8080`.
- Confirm `curl http://localhost:8080/actuator/prometheus` returns metrics.
- Restart Prometheus with `make platform-down` and `make platform-up` if it started before the Java service.

Tests fail because Docker is unavailable:

- Start Docker Desktop or Docker Engine.
- Re-run `make java-test`; some tests use Testcontainers.

Generated protobuf files changed:

- Run `make proto`.
- Review changes under `proto/gen`.
