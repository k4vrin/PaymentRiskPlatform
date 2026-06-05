package dev.kavrin.paymentrisk.consumer.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Converts unrecoverable Kafka consumer failures into durable dead-letter records.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaConsumerFailureHandler {

    private final KafkaDeadLetterRecorder deadLetterRecorder;

    public Mono<Void> handle(
            String consumerName,
            ConsumerRecord<String, String> record,
            Throwable error
    ) {
        return deadLetterRecorder.record(consumerName, record, error)
                .doOnSuccess(ignored -> log.warn(
                        "Dead-lettered Kafka record consumer={} topic={} partition={} offset={} errorType={}",
                        consumerName,
                        record.topic(),
                        record.partition(),
                        record.offset(),
                        error.getClass().getSimpleName()
                ));
    }
}
