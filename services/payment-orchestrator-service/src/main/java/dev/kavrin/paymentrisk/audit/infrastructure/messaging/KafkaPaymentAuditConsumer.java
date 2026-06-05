package dev.kavrin.paymentrisk.audit.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kavrin.paymentrisk.audit.application.PaymentAuditConsumerProperties;
import dev.kavrin.paymentrisk.audit.application.PaymentAuditProjection;
import dev.kavrin.paymentrisk.audit.application.PaymentAuditProjector;
import dev.kavrin.paymentrisk.audit.application.UnsupportedPaymentAuditEventSchemaException;
import dev.kavrin.paymentrisk.consumer.application.IdempotentConsumerGuard;
import dev.kavrin.paymentrisk.consumer.application.ProcessedMessageCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Consumes payment lifecycle Kafka events and projects them into the audit model.
 *
 * <p>This consumer should stay thin: it validates the envelope, delegates
 * duplicate protection to the idempotent consumer guard, and delegates storage
 * to {@link PaymentAuditProjector}.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "payment-risk.kafka.consumers.payment-audit",
        name = "enabled",
        havingValue = "true"
)
public class KafkaPaymentAuditConsumer {

    private final ObjectMapper objectMapper;
    private final PaymentAuditConsumerProperties properties;
    private final IdempotentConsumerGuard consumerGuard;
    private final PaymentAuditProjector projector;

    @KafkaListener(
            groupId = "${payment-risk.kafka.consumers.payment-audit.consumer-name:payment-audit-consumer}",
            topics = {
                    "#{@kafkaTopicProperties.topics().paymentAuthorizationCompleted()}",
                    "#{@kafkaTopicProperties.topics().paymentReversalCompleted()}"
            }
    )
    public void consume(ConsumerRecord<String, String> record) {
        handle(record).block();
    }

    Mono<Void> handle(ConsumerRecord<String, String> record) {
        return Mono.fromCallable(() -> parseProjection(record))
                .flatMap(projection -> {
                    var command = new ProcessedMessageCommand(
                            properties.getConsumerName(),
                            record.topic(),
                            record.partition(),
                            record.offset(),
                            projection.eventId()
                    );

                    return consumerGuard.processOnce(
                            command,
                            projector.project(projection)
                    );
                })
                .doOnNext(processed -> {
                    if (processed) {
                        log.info(
                                "Processed payment audit event topic={} partition={} offset={}",
                                record.topic(),
                                record.partition(),
                                record.offset()
                        );
                    } else {
                        log.info(
                                "Skipped duplicate payment audit event topic={} partition={} offset={}",
                                record.topic(),
                                record.partition(),
                                record.offset()
                        );
                    }
                })
                .then();
    }

    private PaymentAuditProjection parseProjection(ConsumerRecord<String, String> record) throws Exception {
        JsonNode root = objectMapper.readTree(record.value());

        var eventId = requiredText(root, "eventId");
        var schemaVersion = requiredText(root, "schemaVersion");

        if (!properties.getExpectedSchemaVersion().equals(schemaVersion)) {
            throw new UnsupportedPaymentAuditEventSchemaException(eventId, schemaVersion);
        }

        return new PaymentAuditProjection(
                eventId,
                supportedEventType(root),
                requiredText(root, "aggregateId"),
                requiredText(root, "aggregateType"),
                schemaVersion,
                requiredText(root, "correlationId"),
                Instant.parse(requiredText(root, "occurredAt")),
                root.get("payload")
        );
    }

    private static String supportedEventType(JsonNode root) {
        var eventType = requiredText(root, "eventType");

        return switch (eventType) {
            case "PaymentAuthorized", "PaymentDeclined", "PaymentReversed" -> eventType;
            default -> throw new IllegalArgumentException("Unsupported payment audit event type: " + eventType);
        };
    }

    private static String requiredText(JsonNode root, String fieldName) {
        var node = root.get(fieldName);

        if (node == null || node.isNull() || node.asText().isBlank()) {
            throw new IllegalArgumentException("Missing required event envelope field: " + fieldName);
        }

        return node.asText();
    }
}
