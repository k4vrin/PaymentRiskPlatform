package dev.kavrin.paymentrisk.shared.messaging;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class KafkaTopics {

    public static final String PAYMENT_AUTHORIZATION_REQUESTED =
            "payment.authorization.requested";

    public static final String RISK_SCORE_COMPLETED =
            "risk.score.completed";

    public static final String PAYMENT_AUTHORIZATION_COMPLETED =
            "payment.authorization.completed";

    public static final String PAYMENT_REVERSAL_COMPLETED =
            "payment.reversal.completed";

    public static final String PLATFORM_DEAD_LETTER_RECORDED =
            "platform.dead-letter.recorded";

    public static final TopicDefinition PAYMENT_AUTHORIZATION_REQUESTED_DEFINITION =
            new TopicDefinition(
                    PAYMENT_AUTHORIZATION_REQUESTED,
                    "payment-orchestrator-service",
                    List.of("risk-scoring-service", "payment-audit-consumer"),
                    "aggregateId/paymentId"
            );

    public static final TopicDefinition RISK_SCORE_COMPLETED_DEFINITION =
            new TopicDefinition(
                    RISK_SCORE_COMPLETED,
                    "risk-scoring-service",
                    List.of("payment-orchestrator-service"),
                    "aggregateId/paymentId"
            );

    public static final TopicDefinition PAYMENT_AUTHORIZATION_COMPLETED_DEFINITION =
            new TopicDefinition(
                    PAYMENT_AUTHORIZATION_COMPLETED,
                    "payment-orchestrator-service",
                    List.of("payment-audit-consumer", "settlement-projection-consumer", "ops-metrics-consumer"),
                    "aggregateId/paymentId"
            );

    public static final TopicDefinition PAYMENT_REVERSAL_COMPLETED_DEFINITION =
            new TopicDefinition(
                    PAYMENT_REVERSAL_COMPLETED,
                    "payment-orchestrator-service",
                    List.of("payment-audit-consumer", "settlement-projection-consumer", "ops-metrics-consumer"),
                    "aggregateId/paymentId"
            );

    public static final TopicDefinition PLATFORM_DEAD_LETTER_RECORDED_DEFINITION =
            new TopicDefinition(
                    PLATFORM_DEAD_LETTER_RECORDED,
                    "dead-letter-handler",
                    List.of("ops-metrics-consumer", "operations-api"),
                    "aggregateId/originalEventId"
            );

    public static final List<TopicDefinition> ALL = List.of(
            PAYMENT_AUTHORIZATION_REQUESTED_DEFINITION,
            RISK_SCORE_COMPLETED_DEFINITION,
            PAYMENT_AUTHORIZATION_COMPLETED_DEFINITION,
            PAYMENT_REVERSAL_COMPLETED_DEFINITION,
            PLATFORM_DEAD_LETTER_RECORDED_DEFINITION
    );

    public record TopicDefinition(
            String name,
            String producer,
            List<String> consumers,
            String partitionKey
    ) {

        public TopicDefinition {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("name is required");
            }
            if (producer == null || producer.isBlank()) {
                throw new IllegalArgumentException("producer is required");
            }
            if (consumers == null || consumers.isEmpty()) {
                throw new IllegalArgumentException("consumers are required");
            }
            if (consumers.stream().anyMatch(consumer -> consumer == null || consumer.isBlank())) {
                throw new IllegalArgumentException("consumer name is required");
            }
            if (partitionKey == null || partitionKey.isBlank()) {
                throw new IllegalArgumentException("partitionKey is required");
            }

            name = name.trim();
            producer = producer.trim();
            consumers = consumers.stream()
                    .map(String::trim)
                    .toList();
            partitionKey = partitionKey.trim();
        }
    }
}
