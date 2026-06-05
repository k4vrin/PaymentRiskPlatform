package dev.kavrin.paymentrisk.shared.messaging;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaTopicsTest {

    @Test
    void shouldDefineAllPhaseSixTopicNames() {
        assertThat(KafkaTopics.ALL)
                .extracting(KafkaTopics.TopicDefinition::name)
                .containsExactly(
                        KafkaTopics.PAYMENT_AUTHORIZATION_REQUESTED,
                        KafkaTopics.RISK_SCORE_COMPLETED,
                        KafkaTopics.PAYMENT_AUTHORIZATION_COMPLETED,
                        KafkaTopics.PAYMENT_REVERSAL_COMPLETED,
                        KafkaTopics.PLATFORM_DEAD_LETTER_RECORDED
                );
    }

    @Test
    void shouldDefineProducerConsumerOwnershipAndPartitionKeys() {
        assertThat(KafkaTopics.ALL)
                .allSatisfy(topic -> {
                    assertThat(topic.producer()).isNotBlank();
                    assertThat(topic.consumers()).isNotEmpty();
                    assertThat(topic.partitionKey()).isNotBlank();
                });

        assertThat(KafkaTopics.PAYMENT_AUTHORIZATION_REQUESTED_DEFINITION.producer())
                .isEqualTo("payment-orchestrator-service");
        assertThat(KafkaTopics.PAYMENT_AUTHORIZATION_REQUESTED_DEFINITION.consumers())
                .contains("risk-scoring-service");
        assertThat(KafkaTopics.PLATFORM_DEAD_LETTER_RECORDED_DEFINITION.partitionKey())
                .isEqualTo("aggregateId/originalEventId");
    }

    @Test
    void shouldKeepTopicNamesUnique() {
        Set<String> topicNames = KafkaTopics.ALL.stream()
                .map(KafkaTopics.TopicDefinition::name)
                .collect(java.util.stream.Collectors.toSet());

        assertThat(topicNames).hasSameSizeAs(KafkaTopics.ALL);
    }
}
