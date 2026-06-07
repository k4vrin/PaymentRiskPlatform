package dev.kavrin.paymentrisk.security.ratelimit;

import lombok.Getter;

import java.time.Duration;

/**
 * Raised when a caller exceeds the configured request rate limit.
 */
@Getter
public class RateLimitExceededException extends RuntimeException {

    private final long limit;
    private final Duration retryAfter;

    public RateLimitExceededException(long limit, Duration retryAfter) {
        super("Request rate limit exceeded");
        this.limit = limit;
        this.retryAfter = retryAfter;
    }
}