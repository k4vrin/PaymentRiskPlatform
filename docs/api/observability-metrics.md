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
