package dev.kavrin.paymentrisk.ops.application.metrics;

import reactor.core.publisher.Mono;

/**
 * Applies consumed events to durable operational counters.
 */
public interface OpsMetricsProjector {

    Mono<Void> project(OpsMetricsEvent event);
}