package dev.kavrin.paymentrisk.security.ratelimit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfiguration {

    @Bean
    @ConditionalOnMissingBean
    RateLimiter rateLimiter(ReactiveStringRedisTemplate redisTemplate, RateLimitProperties properties) {
        return new RedisFixedWindowRateLimiter(redisTemplate, properties);
    }

    @Bean
    RateLimitWebFilter rateLimitWebFilter(RateLimiter rateLimiter, RateLimitProperties properties) {
        return new RateLimitWebFilter(rateLimiter, properties);
    }
}
