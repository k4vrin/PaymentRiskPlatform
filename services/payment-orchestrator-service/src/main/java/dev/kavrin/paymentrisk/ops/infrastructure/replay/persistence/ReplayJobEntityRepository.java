package dev.kavrin.paymentrisk.ops.infrastructure.replay.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public interface ReplayJobEntityRepository extends ReactiveCrudRepository<ReplayJobEntity, String> {

    Mono<ReplayJobEntity> findFirstBySourceAndTargetIdAndStatusIn(
            String source,
            String targetId,
            Collection<String> statuses
    );

    Flux<ReplayJobEntity> findBySourceAndTargetIdOrderByRequestedAtDesc(
            String source,
            String targetId
    );
}
