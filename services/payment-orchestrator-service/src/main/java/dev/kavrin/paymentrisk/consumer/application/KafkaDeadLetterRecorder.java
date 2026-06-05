package dev.kavrin.paymentrisk.consumer.application;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import reactor.core.publisher.Mono;

/**
 * Persists Kafka records that could not be processed by a consumer.
 */
public interface KafkaDeadLetterRecorder {

    Mono<Void> record(
            String consumerName,
            ConsumerRecord<String, String> record,
            Throwable error
    );
}