# ADR 0002: Use Kafka and RabbitMQ for Distinct Messaging Needs

## Status
Accepted

## Date
2026-05-23

## Context
The Reactive Payment Risk Platform needs asynchronous processing for payment events, audit history, settlement projections, operational replay, partner callbacks, and failure recovery.

These workflows do not all have the same messaging shape:

- Some messages are durable business facts that already happened, such as `PaymentAuthorized` or `PaymentReversed`.
- Some messages are targeted commands that ask one worker group to perform work, such as `CallPartnerWebhook`.
- Some consumers need replay and independent read positions.
- Some work needs queue-style acknowledgement, retry, and dead-letter handling after one worker attempts delivery.

The project is also intended to demonstrate practical enterprise backend experience with Kafka and JMS/MQ-style messaging without pretending they solve the same problem.

## Decision
Use both Kafka and RabbitMQ, with a strict boundary between their responsibilities.

Kafka is the platform event log for durable, replayable business facts:

- payment authorization events;
- payment reversal events;
- risk decision events;
- audit and operational projection inputs;
- dead-letter or poison event streams;
- replayable downstream processing.

RabbitMQ is the command broker for targeted one-worker tasks:

- partner webhook callback commands;
- retryable delivery work;
- explicit acknowledgement after terminal handling;
- dead-letter queue routing after retry exhaustion.

The payment orchestrator will publish business facts through the transactional outbox pattern. Outbox relay workers publish those records to Kafka after the database transaction commits. Kafka consumers then build audit history, settlement projections, metrics, and operational views.

RabbitMQ messages will be created from already-committed business state when a workflow needs a worker to perform a specific task. A RabbitMQ command is not the source of truth for the business event; it is a work item derived from business state.

## Boundaries

Kafka messages are facts:

- "Payment was authorized."
- "Payment was reversed."
- "Risk decision was recorded."

RabbitMQ messages are commands:

- "Call this partner webhook."
- "Retry this callback delivery."

Kafka consumers may be many and independent. RabbitMQ command messages should be handled by one worker group.

Kafka streams are replayable. RabbitMQ commands are short-lived work items with acknowledgement, retry, and DLQ semantics.

Kafka ordering matters by key, such as `paymentId` or `merchantId`. RabbitMQ delivery is optimized around task dispatch and completion.

## Consequences

This gives the project a realistic enterprise messaging design:

- Kafka supports auditability, replay, projections, consumer independence, and recovery.
- RabbitMQ supports command dispatch, acknowledgements, retries, and DLQs for targeted work.
- The architecture makes the difference between events and commands explicit.
- The portfolio demonstrates both event streaming and traditional MQ/JMS-style command processing.

The tradeoff is additional local infrastructure and more operational concepts to document and test. This is acceptable because the project goal includes distributed-system and enterprise messaging competency.

The implementation must avoid blurring the boundary:

- Do not use RabbitMQ as the payment event log.
- Do not use Kafka as a generic command queue for one-off partner callback work.
- Do not make the synchronous payment API response depend on Kafka or RabbitMQ completing downstream work.
- Do not publish messages before the business transaction is durable.

## Rejected Alternatives

- Use only Kafka: rejected because partner callbacks are targeted command work where queue acknowledgement, retry, and DLQ semantics are clearer than modeling commands as replayable facts.
- Use only RabbitMQ: rejected because audit, settlement projection, replay, and independent consumers are better served by a durable event stream.
- Use database polling only: rejected because it hides the event-driven architecture goal and does not demonstrate broker-backed distributed processing.
- Add a third messaging platform: rejected because Kafka and RabbitMQ already cover the required event-streaming and command-queue patterns.

## References

- Stack ADR: `docs/adr/0001-lock-project-stack-and-dependencies.md`
- Project brief Kafka design: `docs/Project.md#kafka-event-design`
- Project brief RabbitMQ command flow: `docs/Project.md#rabbitmqjms-command-flow`
- Event envelope: `docs/events/event-envelope.md`
