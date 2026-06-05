package dev.kavrin.paymentrisk.outbox.application;

import java.time.Instant;

public record OutboxProducerRetryDecision(
        boolean retryable,
        int nextRetryCount,
        Instant nextRetryAt,
        String failureStatus
) {

    public static OutboxProducerRetryDecision retryable(int nextRetryCount, Instant nextRetryAt) {
        return new OutboxProducerRetryDecision(
                true,
                nextRetryCount,
                nextRetryAt,
                "FAILED"
        );
    }

    public static OutboxProducerRetryDecision terminal(int nextRetryCount) {
        return new OutboxProducerRetryDecision(
                false,
                nextRetryCount,
                null,
                "FAILED"
        );
    }
}
