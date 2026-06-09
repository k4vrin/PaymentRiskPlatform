package dev.kavrin.paymentrisk.shared.observability.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IdempotencyCacheMetricsTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final IdempotencyCacheMetrics metrics = new IdempotencyCacheMetrics(meterRegistry);

    @Test
    void recordsRedisCacheAndDatabaseFallbackMetrics() {
        metrics.recordRedisHit("payment_authorization");
        metrics.recordRedisMiss("payment_authorization");
        metrics.recordRedisWriteFailure("payment_authorization");
        metrics.recordDatabaseFallbackHit("payment_authorization");

        assertThat(redisCounter("hit")).isEqualTo(1.0);
        assertThat(redisCounter("miss")).isEqualTo(1.0);
        assertThat(redisCounter("write_failure")).isEqualTo(1.0);
        assertThat(meterRegistry.counter(
                "paymentrisk.idempotency.cache.database.fallbacks",
                "scope", "PAYMENT_AUTHORIZATION"
        ).count()).isEqualTo(1.0);
    }

    private double redisCounter(String result) {
        return meterRegistry.counter(
                "paymentrisk.idempotency.cache.redis.requests",
                "result", result,
                "scope", "PAYMENT_AUTHORIZATION"
        ).count();
    }
}
