package dev.kavrin.paymentrisk.security.ratelimit;

import reactor.core.publisher.Mono;

public interface RateLimiter {

    Mono<RateLimitDecision> check(String key);
}