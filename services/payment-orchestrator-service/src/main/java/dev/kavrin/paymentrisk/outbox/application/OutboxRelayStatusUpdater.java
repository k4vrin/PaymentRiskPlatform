package dev.kavrin.paymentrisk.outbox.application;

import reactor.core.publisher.Mono;

public interface OutboxRelayStatusUpdater {

    Mono<Void> markPublished(String eventId);

    Mono<Void> markFailed(String eventId, String errorMessage);
}