# Phase 5: Operations API

Phase 5 adds operator-facing endpoints for payment investigation, outbox and dead-letter inspection, replay requests,
consumer-lag visibility, and basic ops authorization.

## Local Ops Authentication

Phase 5 uses a local header-backed role mechanism so the operations endpoints can enforce access rules before the fuller
Phase 7 security model is implemented.

- `X-User-Id`: operator identifier used as the replay requester.
- `X-User-Roles`: comma-separated roles. `OPS` and `ADMIN` can access `/api/v1/ops/**`.

Merchant-only and anonymous requests are denied for operations endpoints.

## Endpoints

### Search Payments

`GET /api/v1/ops/payments`

Filters:

- `status`
- `merchantId`
- `customerId`
- `paymentId`
- `createdFrom`
- `createdTo`
- `size`
- `pageToken`

The response excludes raw payment method tokens and raw device fingerprints.

### Inspect Outbox

`GET /api/v1/ops/outbox`

Filters:

- `status`
- `eventType`
- `aggregateId`
- `createdFrom`
- `createdTo`
- `size`
- `pageToken`

Failed outbox rows are ordered by retry urgency. Cursor tokens include a filter hash, so a token cannot be reused after
filters change.

### Inspect Dead Letters

`GET /api/v1/ops/dead-letters`

Filters:

- `sourceSystem`
- `status`
- `destinationName`
- `eventId`
- `messageId`
- `failedFrom`
- `failedTo`
- `size`
- `pageToken`

Dead-letter responses expose payload previews only, not full payloads.

### Request Replay

`POST /api/v1/ops/replay/{source}/{targetId}`

Supported `source` values:

- `OUTBOX`
- `DEAD_LETTER`

Request body:

```json
{
  "reason": "manual retry after downstream recovery"
}
```

Replay behavior:

- Outbox replay is eligible only for `FAILED` outbox events.
- Dead-letter replay is eligible only when `replay_eligible=true`.
- A unique active replay job is enforced per `(source, targetId)`.
- Replay requests create an `ops_replay_jobs` row.
- Replay requests emit an `OpsReplayRequested` outbox audit event.
- Actual Kafka/RabbitMQ replay execution is deferred to Phase 6 messaging work.

### Consumer Lag

`GET /api/v1/ops/consumer-lag`

Filters:

- `consumerGroup`
- `topic`

Phase 5 exposes the read model and API boundary. The default adapter returns an unavailable status until Phase 6 wires
real Kafka consumer-lag inspection.

## Verification

Focused Phase 5 verification:

```bash
cd services/payment-orchestrator-service
./mvnw '-Dtest=dev.kavrin.paymentrisk.ops.**.*Test,dev.kavrin.paymentrisk.security.**.*Test' test
```
