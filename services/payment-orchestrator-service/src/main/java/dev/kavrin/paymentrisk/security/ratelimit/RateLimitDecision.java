package dev.kavrin.paymentrisk.security.ratelimit;

import java.time.Duration;

public record RateLimitDecision(
        boolean allowed,
        long limit,
        long remaining,
        Duration retryAfter
) {
    public static RateLimitDecision allowed(long limit, long remaining) {
        return new RateLimitDecision(true, limit, remaining, Duration.ZERO);
    }

    public static RateLimitDecision rejected(long limit, Duration retryAfter) {
        return new RateLimitDecision(false, limit, 0, retryAfter);
    }
}