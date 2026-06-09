# Observability Metrics

The Java payment orchestrator exposes Micrometer metrics through the actuator/Prometheus stack.

## API Latency

`paymentrisk.api.request.duration` records request latency with low-cardinality tags:

- `method`
- `route`
- `status`
- `status_class`

The `route` tag uses Spring's best matching route pattern, such as `/api/v1/payments/{paymentId}`, not raw request
paths. Do not add payment IDs, merchant IDs, customer IDs, idempotency keys, correlation IDs, or raw paths as metric
tags.

## Authorization Metrics

- `paymentrisk.payment.authorization.attempts`
- `paymentrisk.payment.authorization.outcomes` tagged by `outcome`
- `paymentrisk.payment.authorization.idempotency.replays`
- `paymentrisk.payment.authorization.declines` tagged by `reason_code`

`REVIEW_REQUIRED` is counted as an authorization outcome even though the current Phase 7 domain behavior maps it to a
declined payment with a `REVIEW_REQUIRED` reason code.

## Risk Metrics

- `paymentrisk.risk.service.duration`
- `paymentrisk.risk.service.timeouts`
- `paymentrisk.risk.service.unavailable`

Risk latency is recorded around the Java risk-client call. Timeout and unavailable outcomes are counted before they are
mapped to structured API errors.

## Idempotency And Redis Metrics

- `paymentrisk.idempotency.cache.redis.requests` tagged by `scope` and `result`
- `paymentrisk.idempotency.cache.database.fallbacks` tagged by `scope`

Redis request results use `hit`, `miss`, and `write_failure`. Database fallback hits are counted when a duplicate
completed idempotency response is served from durable storage after the Redis path did not produce a matching snapshot.

## Messaging Metrics

- `payment_risk_outbox_publish_total` tagged by `event_type` and `result`
- `payment_risk_outbox_lag_seconds` tagged by `event_type`
- `payment_risk_consumer_events_total` tagged by `consumer`, `event_type`, and `result`
- `payment_risk_consumer_lag_records` tagged by `consumer` and `topic`
- `payment_risk_dead_letters_total` tagged by `source`
- `payment_risk_replay_requests_total` tagged by `source` and `result`
- `payment_risk_partner_callback_total` tagged by `callback_type` and `result`
