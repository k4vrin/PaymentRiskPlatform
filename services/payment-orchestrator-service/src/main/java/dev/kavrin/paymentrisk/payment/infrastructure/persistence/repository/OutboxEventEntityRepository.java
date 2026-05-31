package dev.kavrin.paymentrisk.payment.infrastructure.persistence.repository;

import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.OutboxEventEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.time.Instant;

public interface OutboxEventEntityRepository
        extends ReactiveCrudRepository<OutboxEventEntity, String> {

    Flux<OutboxEventEntity> findByStatusAndNextRetryAtLessThanEqual(
            String status,
            Instant nextRetryAt
    );
}