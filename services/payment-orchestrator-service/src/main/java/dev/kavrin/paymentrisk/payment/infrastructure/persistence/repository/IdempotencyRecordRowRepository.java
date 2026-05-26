package dev.kavrin.paymentrisk.payment.infrastructure.persistence.repository;

import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.IdempotencyRecordRow;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface IdempotencyRecordRowRepository
        extends ReactiveCrudRepository<IdempotencyRecordRow, String> {

    Mono<IdempotencyRecordRow> findByScopeAndIdempotencyKey(
            String scope,
            String idempotencyKey
    );
}