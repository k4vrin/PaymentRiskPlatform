package dev.kavrin.paymentrisk.shared.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaTopicPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(KafkaMessagingConfiguration.class);

    @Test
    void shouldUseDefaultTopicConfiguration() {
        contextRunner.run(context -> {
            var properties = context.getBean(KafkaTopicProperties.class);

            assertThat(properties.topics().paymentAuthorizationRequested())
                    .isEqualTo(KafkaTopics.PAYMENT_AUTHORIZATION_REQUESTED);
            assertThat(properties.topics().riskScoreCompleted())
                    .isEqualTo(KafkaTopics.RISK_SCORE_COMPLETED);
            assertThat(properties.topics().paymentAuthorizationCompleted())
                    .isEqualTo(KafkaTopics.PAYMENT_AUTHORIZATION_COMPLETED);
            assertThat(properties.topics().paymentReversalCompleted())
                    .isEqualTo(KafkaTopics.PAYMENT_REVERSAL_COMPLETED);
            assertThat(properties.topics().platformDeadLetterRecorded())
                    .isEqualTo(KafkaTopics.PLATFORM_DEAD_LETTER_RECORDED);
            assertThat(properties.topicAdmin().enabled()).isFalse();
            assertThat(properties.topicAdmin().partitions()).isEqualTo(1);
            assertThat(properties.topicAdmin().replicas()).isEqualTo((short) 1);
        });
    }

    @Test
    void shouldBindKafkaTopicOverrides() {
        contextRunner
                .withPropertyValues(
                        "payment-risk.kafka.topics.payment-authorization-requested=custom.authorization.requested",
                        "payment-risk.kafka.topics.risk-score-completed=custom.risk.completed",
                        "payment-risk.kafka.topics.payment-authorization-completed=custom.authorization.completed",
                        "payment-risk.kafka.topics.payment-reversal-completed=custom.reversal.completed",
                        "payment-risk.kafka.topics.platform-dead-letter-recorded=custom.dead-letter.recorded",
                        "payment-risk.kafka.topic-admin.enabled=true",
                        "payment-risk.kafka.topic-admin.partitions=6",
                        "payment-risk.kafka.topic-admin.replicas=2"
                )
                .run(context -> {
                    var properties = context.getBean(KafkaTopicProperties.class);

                    assertThat(properties.topics().paymentAuthorizationRequested())
                            .isEqualTo("custom.authorization.requested");
                    assertThat(properties.topics().riskScoreCompleted())
                            .isEqualTo("custom.risk.completed");
                    assertThat(properties.topics().paymentAuthorizationCompleted())
                            .isEqualTo("custom.authorization.completed");
                    assertThat(properties.topics().paymentReversalCompleted())
                            .isEqualTo("custom.reversal.completed");
                    assertThat(properties.topics().platformDeadLetterRecorded())
                            .isEqualTo("custom.dead-letter.recorded");
                    assertThat(properties.topicAdmin().enabled()).isTrue();
                    assertThat(properties.topicAdmin().partitions()).isEqualTo(6);
                    assertThat(properties.topicAdmin().replicas()).isEqualTo((short) 2);
                });
    }

    @Test
    void shouldRejectBlankTopicName() {
        contextRunner
                .withPropertyValues("payment-risk.kafka.topics.payment-authorization-requested=")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void shouldNotCreateAdminTopicsByDefault() {
        contextRunner.run(context ->
                assertThat(context).doesNotHaveBean(NewTopic.class)
        );
    }

    @Test
    void shouldCreateAdminTopicsWhenEnabled() {
        contextRunner
                .withPropertyValues(
                        "payment-risk.kafka.topic-admin.enabled=true",
                        "payment-risk.kafka.topic-admin.partitions=3",
                        "payment-risk.kafka.topic-admin.replicas=2"
                )
                .run(context -> {
                    assertThat(context).hasBean("paymentAuthorizationRequestedTopic");
                    assertThat(context).hasBean("riskScoreCompletedTopic");
                    assertThat(context).hasBean("paymentAuthorizationCompletedTopic");
                    assertThat(context).hasBean("paymentReversalCompletedTopic");
                    assertThat(context).hasBean("platformDeadLetterRecordedTopic");

                    var topic = context.getBean("paymentAuthorizationRequestedTopic", NewTopic.class);

                    assertThat(topic.name()).isEqualTo(KafkaTopics.PAYMENT_AUTHORIZATION_REQUESTED);
                    assertThat(topic.numPartitions()).isEqualTo(3);
                    assertThat(topic.replicationFactor()).isEqualTo((short) 2);
                });
    }
}
