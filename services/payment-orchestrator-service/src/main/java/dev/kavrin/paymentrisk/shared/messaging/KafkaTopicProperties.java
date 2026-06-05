package dev.kavrin.paymentrisk.shared.messaging;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "payment-risk.kafka")
public record KafkaTopicProperties(
        @Valid Topics topics,
        @Valid TopicAdmin topicAdmin
) {

    public KafkaTopicProperties {
        topics = topics == null ? Topics.defaults() : topics;
        topicAdmin = topicAdmin == null ? TopicAdmin.defaults() : topicAdmin;
    }

    public record Topics(
            @NotBlank String paymentAuthorizationRequested,
            @NotBlank String riskScoreCompleted,
            @NotBlank String paymentAuthorizationCompleted,
            @NotBlank String paymentReversalCompleted,
            @NotBlank String platformDeadLetterRecorded
    ) {

        static Topics defaults() {
            return new Topics(
                    KafkaTopics.PAYMENT_AUTHORIZATION_REQUESTED,
                    KafkaTopics.RISK_SCORE_COMPLETED,
                    KafkaTopics.PAYMENT_AUTHORIZATION_COMPLETED,
                    KafkaTopics.PAYMENT_REVERSAL_COMPLETED,
                    KafkaTopics.PLATFORM_DEAD_LETTER_RECORDED
            );
        }
    }

    public record TopicAdmin(
            boolean enabled,
            @Min(1) int partitions,
            @Min(1) short replicas
    ) {

        static TopicAdmin defaults() {
            // Keep admin creation off unless an environment opts in. Local Compose
            // can enable it, but production usually manages topics with platform IaC.
            return new TopicAdmin(false, 1, (short) 1);
        }
    }
}
