package dev.kavrin.paymentrisk.outbox.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class OutboxProducerRetryPolicy {

    private final OutboxRelayProperties properties;

    public OutboxProducerRetryDecision decide(int currentRetryCount, Instant failedAt) {
        if (failedAt == null) {
            failedAt = Instant.now();
        }

        var nextRetryCount = currentRetryCount + 1;

        if (nextRetryCount >= properties.getMaxAttempts()) {
            return OutboxProducerRetryDecision.terminal(nextRetryCount);
        }

        var backoffMillis = computeBackoffMillis(nextRetryCount);
        var nextRetryAt = failedAt.plusMillis(backoffMillis);

        return OutboxProducerRetryDecision.retryable(nextRetryCount, nextRetryAt);
    }


    private long computeBackoffMillis(int attemptNumber) {
        // Exponential backoff: attempt 1 waits 1x, attempt 2 waits 2x,
        // attempt 3 waits 4x, capped so retries do not grow forever.
        var multiplier = 1L << Math.max(0, attemptNumber - 1);
        var backoff = properties.getInitialBackoffMillis() * multiplier;

        return Math.min(backoff, properties.getMaxBackoffMillis());
    }
}
