package dev.kavrin.paymentrisk.outbox.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.kavrin.paymentrisk.outbox.application.OutboxEventPublisher;
import dev.kavrin.paymentrisk.outbox.domain.OutboxEvent;
import dev.kavrin.paymentrisk.shared.messaging.KafkaTopicProperties;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
@ConditionalOnProperty(prefix = "payment-risk.outbox.relay", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class KafkaOutboxEventPublisher implements OutboxEventPublisher {

    private final ObjectMapper objectMapper;
    private final KafkaTopicProperties topicProperties;
    private final KafkaRecordSender sender;

    @Override
    public Mono<Void> publish(OutboxEvent event) {
        var topic = topicFor(event.eventType());
        var key = event.aggregateId();

        return Mono.fromCallable(() -> envelopeJson(event))
                .flatMap(value -> {
                    var record = new ProducerRecord<String, String>(topic, key, value);

                    addHeader(record, "event_id", event.eventId());
                    addHeader(record, "event_type", event.eventType());
                    addHeader(record, "schema_version", event.schemaVersion());
                    addHeader(record, "aggregate_id", event.aggregateId());
                    addHeader(record, "aggregate_type", event.aggregateType());
                    addHeader(record, "correlation_id", event.correlationId());

                    return sender.send(record);
                });
    }

    private String envelopeJson(OutboxEvent event) throws Exception {
        ObjectNode envelope = objectMapper.createObjectNode();
        envelope.put("eventId", event.eventId());
        envelope.put("schemaVersion", event.schemaVersion());
        envelope.put("eventType", event.eventType());
        envelope.put("aggregateId", event.aggregateId());
        envelope.put("aggregateType", event.aggregateType());
        envelope.put("occurredAt", event.occurredAt().toString());
        envelope.put("producer", event.producer());
        envelope.put("correlationId", event.correlationId());
        envelope.set("payload", objectMapper.readTree(event.payloadJson()));

        return objectMapper.writeValueAsString(envelope);
    }

    private String topicFor(String eventType) {
        return switch (eventType) {
            case "PaymentAuthorizationRequested" -> topicProperties.topics().paymentAuthorizationRequested();
            case "RiskScoreCompleted" -> topicProperties.topics().riskScoreCompleted();
            case "PaymentAuthorized", "PaymentDeclined" -> topicProperties.topics().paymentAuthorizationCompleted();
            case "PaymentReversed" -> topicProperties.topics().paymentReversalCompleted();
            case "DeadLetterRecorded" -> topicProperties.topics().platformDeadLetterRecorded();
            default -> throw new IllegalArgumentException("Unsupported outbox event type: " + eventType);
        };
    }

    private static void addHeader(ProducerRecord<String, String> record, String name, String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        record.headers().add(new RecordHeader(name, value.getBytes(StandardCharsets.UTF_8)));
    }
}
