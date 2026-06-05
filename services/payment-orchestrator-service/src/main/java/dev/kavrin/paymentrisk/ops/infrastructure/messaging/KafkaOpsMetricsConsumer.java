package dev.kavrin.paymentrisk.ops.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kavrin.paymentrisk.consumer.application.IdempotentConsumerGuard;
import dev.kavrin.paymentrisk.consumer.application.KafkaConsumerFailureHandler;
import dev.kavrin.paymentrisk.consumer.application.ProcessedMessageCommand;
import dev.kavrin.paymentrisk.ops.application.metrics.OpsMetricsConsumerProperties;
import dev.kavrin.paymentrisk.ops.application.metrics.OpsMetricsEvent;
import dev.kavrin.paymentrisk.ops.application.metrics.OpsMetricsProjector;
import dev.kavrin.paymentrisk.ops.application.metrics.UnsupportedOpsMetricsEventSchemaException;
import dev.kavrin.paymentrisk.shared.messaging.MessagingObservability;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Consumes selected platform/payment events and updates durable ops metrics.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "payment-risk.kafka.consumers.ops-metrics",
        name = "enabled",
        havingValue = "true"
)
public class KafkaOpsMetricsConsumer {

    private final ObjectMapper objectMapper;
    private final OpsMetricsConsumerProperties properties;
    private final IdempotentConsumerGuard consumerGuard;
    private final OpsMetricsProjector projector;
    private final KafkaConsumerFailureHandler failureHandler;
    private final MessagingObservability observability;

    @KafkaListener(
            groupId = "${payment-risk.kafka.consumers.ops-metrics.consumer-name:ops-metrics-consumer}",
            topics = {
                    "#{@kafkaTopicProperties.topics().paymentAuthorizationRequested()}",
                    "#{@kafkaTopicProperties.topics().riskScoreCompleted()}",
                    "#{@kafkaTopicProperties.topics().paymentAuthorizationCompleted()}",
                    "#{@kafkaTopicProperties.topics().paymentReversalCompleted()}",
                    "#{@kafkaTopicProperties.topics().platformDeadLetterRecorded()}"
            }
    )
    public void consume(ConsumerRecord<String, String> record) {
        handle(record)
                .onErrorResume(error -> failureHandler.handle(
                        properties.getConsumerName(),
                        record,
                        error
                ))
                .block();
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
                        observability.recordConsumerProcessed(properties.getConsumerName(), "ops-metrics");
                        log.info(
                                "Processed ops metrics event topic={} partition={} offset={}",
                                record.topic(),
                                record.partition(),
                                record.offset()
                        );
                    } else {
                        observability.recordConsumerSkipped(properties.getConsumerName(), "ops-metrics");
                        log.info(
                                "Skipped duplicate ops metrics event topic={} partition={} offset={}",
                                record.topic(),
                                record.partition(),
                                record.offset()
                        );
                    }
                })
                .then();
    }

    private OpsMetricsEvent parseEvent(ConsumerRecord<String, String> record) throws Exception {
        JsonNode root = objectMapper.readTree(record.value());

        var eventId = requiredText(root, "eventId");
        var schemaVersion = requiredText(root, "schemaVersion");

        if (!properties.getExpectedSchemaVersion().equals(schemaVersion)) {
            throw new UnsupportedOpsMetricsEventSchemaException(eventId, schemaVersion);
        }

        return new OpsMetricsEvent(
                eventId,
                requiredText(root, "eventType"),
                requiredText(root, "aggregateId"),
                requiredText(root, "aggregateType"),
                schemaVersion,
                requiredText(root, "correlationId"),
                Instant.parse(requiredText(root, "occurredAt")),
                requiredNode(root, "payload")
        );
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
