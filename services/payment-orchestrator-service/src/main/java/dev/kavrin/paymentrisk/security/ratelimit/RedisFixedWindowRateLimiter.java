package dev.kavrin.paymentrisk.security.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Redis-backed fixed-window rate limiter.
 *
 * <p>Algorithm:
 * - Increment a Redis counter for the merchant.
 * - If this is the first request in the window, attach an expiry.
 * - Allow requests while the counter is less than or equal to the limit.
 * - Reject requests after the limit is exceeded.</p>
 *
 * <p>This implementation is simple and good for portfolio/interview discussion.
 * For stricter production behavior, a Lua script is better because INCR + EXPIRE
 * can be made fully atomic.</p>
 */
@Component
@RequiredArgsConstructor
public class RedisFixedWindowRateLimiter implements RateLimiter {

    private static final String KEY_PREFIX = "rate-limit:";

    private final ReactiveStringRedisTemplate redisTemplate;
    private final RateLimitProperties properties;

    @Override
    public Mono<RateLimitDecision> check(String key) {
        var redisKey = KEY_PREFIX + key;
        var limit = properties.merchantLimit();
        var window = properties.window();

        return redisTemplate.opsForValue()
                .increment(redisKey)
                .flatMap(currentCount -> ensureExpiry(redisKey, currentCount, window)
                        .then(toDecision(redisKey, currentCount, limit)));
    }

    private Mono<Boolean> ensureExpiry(String redisKey, Long currentCount, Duration window) {
        if (currentCount == 1L) {
            return redisTemplate.expire(redisKey, window);
        }

        return Mono.just(false);
    }

    private Mono<RateLimitDecision> toDecision(String redisKey, Long currentCount, long limit) {
        if (currentCount <= limit) {
            return Mono.just(RateLimitDecision.allowed(limit, limit - currentCount));
        }

        return redisTemplate.getExpire(redisKey)
                .defaultIfEmpty(Duration.ZERO)
                .map(retryAfter -> RateLimitDecision.rejected(limit, retryAfter));
    }
}