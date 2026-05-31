package dev.kavrin.paymentrisk.idempotency.infrastructure.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface IdempotencyRecordEntityRepository
        extends ReactiveCrudRepository<IdempotencyRecordEntity, String> {

    Mono<IdempotencyRecordEntity> findByScopeAndIdempotencyKey(
            String scope,
            String idempotencyKey
    );
}
