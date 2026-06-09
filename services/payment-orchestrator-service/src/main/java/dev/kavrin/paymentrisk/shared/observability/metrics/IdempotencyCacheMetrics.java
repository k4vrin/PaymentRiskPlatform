package dev.kavrin.paymentrisk.shared.observability.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IdempotencyCacheMetrics {

    private final MeterRegistry meterRegistry;

    public void recordRedisHit(String scope) {
        recordRedis("hit", scope);
    }

    public void recordRedisMiss(String scope) {
        recordRedis("miss", scope);
    }

    public void recordRedisWriteFailure(String scope) {
        recordRedis("write_failure", scope);
    }

    public void recordDatabaseFallbackHit(String scope) {
        meterRegistry.counter(
                "paymentrisk.idempotency.cache.database.fallbacks",
                "scope", normalize(scope)
        ).increment();
    }

    private void recordRedis(String result, String scope) {
        meterRegistry.counter(
                "paymentrisk.idempotency.cache.redis.requests",
                "result", result,
                "scope", normalize(scope)
        ).increment();
    }

    private String normalize(String scope) {
        if (scope == null || scope.isBlank()) {
            return "UNKNOWN";
        }

        return scope.trim().toUpperCase();
    }
}
