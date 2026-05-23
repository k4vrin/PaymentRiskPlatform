# Event Envelope Versioning

All Kafka business events use a stable envelope around an event-specific payload. The envelope gives consumers enough metadata to route, deduplicate, audit, replay, and evolve events without parsing the payload first.

## Envelope Fields

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `schemaVersion` | string | yes | Version of the event payload schema, using a stable value such as `v1`. Increment only when the payload contract changes. |
| `eventId` | string | yes | Globally unique event identifier. Producers generate this once and consumers use it for idempotency. |
| `eventType` | string | yes | Stable event name such as `PaymentAuthorized`, `PaymentDeclined`, or `PaymentReversed`. |
| `aggregateId` | string | yes | Identifier of the aggregate the event belongs to, usually `paymentId`. |
| `aggregateType` | string | yes | Aggregate category, for example `PAYMENT`, `RISK_DECISION`, or `DEAD_LETTER`. |
| `occurredAt` | string | yes | UTC timestamp for when the business fact occurred, formatted as ISO-8601. |
| `producer` | string | yes | Service or worker that produced the event, for example `payment-orchestrator-service` or `event-relay-worker`. |
| `correlationId` | string | yes | Request or workflow correlation ID propagated from the originating API call or command. |
| `payload` | object | yes | Event-specific data. Payload fields are versioned by `schemaVersion`. |

## Versioning Rules

- Start every new event payload at `schemaVersion: "v1"`.
- Additive payload changes may stay on the same version when existing consumers can safely ignore the new field.
- Breaking payload changes require a new schema version.
- Do not rename or remove envelope fields.
- Do not reuse `eventType` for a different business meaning.
- Keep enum-like payload values stable and document any new values.
- Prefer publishing a new event type over changing the meaning of an existing one.

## Identity And Idempotency

- `eventId` is unique for each published event.
- Retried publishes of the same outbox row must reuse the same `eventId`.
- Consumers must store processed `eventId` values or otherwise make handling idempotent.
- `aggregateId` is the default Kafka partition key for payment lifecycle events to preserve per-payment ordering.

## Example

```json
{
  "schemaVersion": "v1",
  "eventId": "018f4f4f-8df2-7b14-a8fa-7bf785f2d70d",
  "eventType": "PaymentAuthorized",
  "aggregateId": "pay_01HYX8J6K0T4F4DM9M9Y5D5YME",
  "aggregateType": "PAYMENT",
  "occurredAt": "2026-05-23T16:30:00Z",
  "producer": "payment-orchestrator-service",
  "correlationId": "corr_01HYX8J20EXAMPLE",
  "payload": {
    "paymentId": "pay_01HYX8J6K0T4F4DM9M9Y5D5YME",
    "merchantId": "merchant_123",
    "amountMinor": 125000,
    "currency": "USD",
    "authorizationCode": "AUTH123456"
  }
}
```
