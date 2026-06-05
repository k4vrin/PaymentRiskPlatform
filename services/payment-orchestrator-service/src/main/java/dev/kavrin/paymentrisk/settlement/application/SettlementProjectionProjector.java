package dev.kavrin.paymentrisk.settlement.application;

import reactor.core.publisher.Mono;

/**
 * Applies payment outcome events to the settlement read model.
 */
public interface SettlementProjectionProjector {

    Mono<Void> project(SettlementProjectionEvent event);
}