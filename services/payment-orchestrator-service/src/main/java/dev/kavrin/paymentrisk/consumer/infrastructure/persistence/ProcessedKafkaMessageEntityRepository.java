package dev.kavrin.paymentrisk.consumer.infrastructure.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface ProcessedKafkaMessageEntityRepository
        extends ReactiveCrudRepository<ProcessedKafkaMessageEntity, String> {

    Mono<Boolean> existsByConsumerNameAndEventId(String consumerName, String eventId);
}
