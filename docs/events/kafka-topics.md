# Kafka Topic Ownership

Phase 6 event publishing uses stable Kafka topic names. Topic ownership stays explicit so relay, consumer, and ops work can evolve without guessing producer boundaries.

| Topic | Producer | Consumers | Partition key |
| --- | --- | --- | --- |
| `payment.authorization.requested` | `payment-orchestrator-service` | `risk-scoring-service`, `payment-audit-consumer` | `aggregateId` / `paymentId` |
| `risk.score.completed` | `risk-scoring-service` | `payment-orchestrator-service` | `aggregateId` / `paymentId` |
| `payment.authorization.completed` | `payment-orchestrator-service` | `payment-audit-consumer`, `settlement-projection-consumer`, `ops-metrics-consumer` | `aggregateId` / `paymentId` |
| `payment.reversal.completed` | `payment-orchestrator-service` | `payment-audit-consumer`, `settlement-projection-consumer`, `ops-metrics-consumer` | `aggregateId` / `paymentId` |
| `platform.dead-letter.recorded` | `dead-letter-handler` | `ops-metrics-consumer`, `operations-api` | `aggregateId` / `originalEventId` |

## Partitioning Rules

- Use `aggregateId` as default Kafka key for payment and risk lifecycle events.
- For payment lifecycle events, `aggregateId` is the `paymentId`, preserving per-payment ordering.
- For dead-letter events, use the original event ID when no durable aggregate ID is available.
- Do not partition by `correlationId`; one workflow can involve multiple aggregates and should not force unrelated events into the same partition.

## Spring Configuration

Runtime topic names bind through `payment-risk.kafka.topics.*` into `KafkaTopicProperties`.
The Java constants in `KafkaTopics` define the default contract names, while profile YAML can override them when an environment needs prefixed or namespaced topics.

Kafka admin topic creation is controlled by `payment-risk.kafka.topic-admin.enabled`. It is disabled by default because production clusters often create topics through infrastructure tooling. When enabled, Spring registers `NewTopic` beans using the configured partition and replica counts.
