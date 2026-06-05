package dev.kavrin.paymentrisk.shared.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(KafkaTopicProperties.class)
public class KafkaMessagingConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "payment-risk.kafka.topic-admin", name = "enabled", havingValue = "true")
    NewTopic paymentAuthorizationRequestedTopic(KafkaTopicProperties properties) {
        return newTopic(properties.topics().paymentAuthorizationRequested(), properties.topicAdmin());
    }

    @Bean
    @ConditionalOnProperty(prefix = "payment-risk.kafka.topic-admin", name = "enabled", havingValue = "true")
    NewTopic riskScoreCompletedTopic(KafkaTopicProperties properties) {
        return newTopic(properties.topics().riskScoreCompleted(), properties.topicAdmin());
    }

    @Bean
    @ConditionalOnProperty(prefix = "payment-risk.kafka.topic-admin", name = "enabled", havingValue = "true")
    NewTopic paymentAuthorizationCompletedTopic(KafkaTopicProperties properties) {
        return newTopic(properties.topics().paymentAuthorizationCompleted(), properties.topicAdmin());
    }

    @Bean
    @ConditionalOnProperty(prefix = "payment-risk.kafka.topic-admin", name = "enabled", havingValue = "true")
    NewTopic paymentReversalCompletedTopic(KafkaTopicProperties properties) {
        return newTopic(properties.topics().paymentReversalCompleted(), properties.topicAdmin());
    }

    @Bean
    @ConditionalOnProperty(prefix = "payment-risk.kafka.topic-admin", name = "enabled", havingValue = "true")
    NewTopic platformDeadLetterRecordedTopic(KafkaTopicProperties properties) {
        return newTopic(properties.topics().platformDeadLetterRecorded(), properties.topicAdmin());
    }

    private NewTopic newTopic(String name, KafkaTopicProperties.TopicAdmin admin) {
        // Topic names are business contract; partition and replica counts are
        // environment capacity choices, so they stay configurable.
        return new NewTopic(name, admin.partitions(), admin.replicas());
    }
}
