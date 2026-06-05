package dev.kavrin.paymentrisk.outbox.application;

import reactor.core.publisher.Mono;

/**
 * Updates durable outbox state after relay processing.
 * <p>
 * Implementations are responsible for recording
 * publish success, retry scheduling, and terminal
 * failure transitions.
 */
public interface OutboxRelayStatusUpdater {

    Mono<Void> markPublished(String eventId);

    Mono<Void> markFailure(
            String eventId,
            OutboxProducerRetryDecision decision,
            String errorMessage
    );
}