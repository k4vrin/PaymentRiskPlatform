package dev.kavrin.paymentrisk.security.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.mockito.Mockito.*;

class RedisFixedWindowRateLimiterTest {

    @Test
    void allowsUntilLimitThenRejectsAndAllowsAfterWindowReset() {
        var redisTemplate = mock(ReactiveStringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ReactiveValueOperations<String, String> valueOperations = mock(ReactiveValueOperations.class);
        var properties = new RateLimitProperties(true, 1, Duration.ofMinutes(1), "/api/v1/payments");
        var limiter = new RedisFixedWindowRateLimiter(redisTemplate, properties);
        var redisKey = "rate-limit:merchant:merchant_123";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(redisKey))
                .thenReturn(Mono.just(1L))
                .thenReturn(Mono.just(2L))
                .thenReturn(Mono.just(1L));
        when(redisTemplate.expire(redisKey, Duration.ofMinutes(1))).thenReturn(Mono.just(true));
        when(redisTemplate.getExpire(redisKey)).thenReturn(Mono.just(Duration.ofSeconds(45)));

        StepVerifier.create(limiter.check("merchant:merchant_123"))
                .expectNext(RateLimitDecision.allowed(1, 0))
                .verifyComplete();

        StepVerifier.create(limiter.check("merchant:merchant_123"))
                .expectNext(RateLimitDecision.rejected(1, Duration.ofSeconds(45)))
                .verifyComplete();

        StepVerifier.create(limiter.check("merchant:merchant_123"))
                .expectNext(RateLimitDecision.allowed(1, 0))
                .verifyComplete();

        verify(redisTemplate, times(2)).expire(redisKey, Duration.ofMinutes(1));
        verify(redisTemplate).getExpire(redisKey);
    }
}
