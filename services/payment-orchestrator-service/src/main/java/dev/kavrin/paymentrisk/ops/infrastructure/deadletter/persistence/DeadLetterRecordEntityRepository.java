package dev.kavrin.paymentrisk.ops.infrastructure.deadletter.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface DeadLetterRecordEntityRepository
        extends ReactiveCrudRepository<DeadLetterRecordEntity, String> {

    Flux<DeadLetterRecordEntity> findBySourceSystemAndStatusOrderByFailedAtDesc(
            String sourceSystem,
            String status
    );

    Flux<DeadLetterRecordEntity> findByEventId(String eventId);

    Flux<DeadLetterRecordEntity> findByMessageId(String messageId);
}
