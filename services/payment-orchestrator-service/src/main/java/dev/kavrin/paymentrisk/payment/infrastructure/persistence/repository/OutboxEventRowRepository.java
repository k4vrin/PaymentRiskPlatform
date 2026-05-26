package dev.kavrin.paymentrisk.payment.infrastructure.persistence.repository;

import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.OutboxEventRow;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.time.Instant;

public interface OutboxEventRowRepository
        extends ReactiveCrudRepository<OutboxEventRow, String> {

    Flux<OutboxEventRow> findByStatusAndNextRetryAtLessThanEqual(
            String status,
            Instant nextRetryAt
    );
}