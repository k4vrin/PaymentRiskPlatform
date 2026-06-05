package dev.kavrin.paymentrisk.ops.infrastructure.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/**
 * Reactive repository for ops event metric counters.
 */
public interface OpsEventMetricRepository
        extends ReactiveCrudRepository<OpsEventMetricEntity, Long> {

    Mono<OpsEventMetricEntity> findByMetricKey(String metricKey);
}