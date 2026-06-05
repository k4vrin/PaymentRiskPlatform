package dev.kavrin.paymentrisk.settlement.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kavrin.paymentrisk.consumer.application.IdempotentConsumerGuard;
import dev.kavrin.paymentrisk.consumer.application.ProcessedMessageCommand;
import dev.kavrin.paymentrisk.settlement.application.SettlementProjectionConsumerProperties;
import dev.kavrin.paymentrisk.settlement.application.SettlementProjectionEvent;
import dev.kavrin.paymentrisk.settlement.application.SettlementProjectionProjector;
import dev.kavrin.paymentrisk.settlement.application.UnsupportedSettlementEventSchemaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Consumes final payment outcome events and updates settlement projections.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "payment-risk.kafka.consumers.settlement-projection",
        name = "enabled",
        havingValue = "true"
)
public class KafkaSettlementProjectionConsumer {

    private final ObjectMapper objectMapper;
    private final SettlementProjectionConsumerProperties properties;
    private final IdempotentConsumerGuard consumerGuard;
    private final SettlementProjectionProjector projector;

    @KafkaListener(
            groupId = "${payment-risk.kafka.consumers.settlement-projection.consumer-name:settlement-projection-consumer}",
            topics = {
                    "#{@kafkaTopicProperties.topics().paymentAuthorizationCompleted()}",
                    "#{@kafkaTopicProperties.topics().paymentReversalCompleted()}"
            }
    )
    public void consume(ConsumerRecord<String, String> record) {
        handle(record).block();
    }

    Mono<Void> handle(ConsumerRecord<String, String> record) {
        return Mono.fromCallable(() -> parseEvent(record))
                .flatMap(event -> {
                    var command = new ProcessedMessageCommand(
                            properties.getConsumerName(),
                            record.topic(),
                            record.partition(),
                            record.offset(),
                            event.eventId()
                    );

                    return consumerGuard.processOnce(
                            command,
                            projector.project(event)
                    );
                })
                .doOnNext(processed -> {
                    if (processed) {
                        log.info(
                                "Processed settlement projection event topic={} partition={} offset={}",
                                record.topic(),
                                record.partition(),
                                record.offset()
                        );
                    } else {
                        log.info(
                                "Skipped duplicate settlement projection event topic={} partition={} offset={}",
                                record.topic(),
                                record.partition(),
                                record.offset()
                        );
                    }
                })
                .then();
    }

    private SettlementProjectionEvent parseEvent(ConsumerRecord<String, String> record) throws Exception {
        JsonNode root = objectMapper.readTree(record.value());

        var eventId = requiredText(root, "eventId");
        var schemaVersion = requiredText(root, "schemaVersion");

        if (!properties.getExpectedSchemaVersion().equals(schemaVersion)) {
            throw new UnsupportedSettlementEventSchemaException(eventId, schemaVersion);
        }

        var eventType = requiredText(root, "eventType");

        if (!isSupportedSettlementEvent(eventType)) {
            throw new IllegalArgumentException("Unsupported settlement event type: " + eventType);
        }

        return new SettlementProjectionEvent(
                eventId,
                eventType,
                requiredText(root, "aggregateId"),
                requiredText(root, "aggregateType"),
                schemaVersion,
                requiredText(root, "correlationId"),
                Instant.parse(requiredText(root, "occurredAt")),
                requiredNode(root, "payload")
        );
    }

    private static boolean isSupportedSettlementEvent(String eventType) {
        return switch (eventType) {
            case "PaymentAuthorized", "PaymentDeclined", "PaymentReversed" -> true;
            default -> false;
        };
    }

    private static String requiredText(JsonNode root, String fieldName) {
        var node = root.get(fieldName);

        if (node == null || node.isNull() || node.asText().isBlank()) {
            throw new IllegalArgumentException("Missing required event envelope field: " + fieldName);
        }

        return node.asText();
    }

    private static JsonNode requiredNode(JsonNode root, String fieldName) {
        var node = root.get(fieldName);

        if (node == null || node.isNull()) {
            throw new IllegalArgumentException("Missing required event envelope field: " + fieldName);
        }

        return node;
    }
}
