package dev.kavrin.paymentrisk.idempotency.infrastructure.redis;

import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyScope;
import dev.kavrin.paymentrisk.shared.observability.metrics.IdempotencyCacheMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@ConditionalOnBean(ReactiveStringRedisTemplate.class)
@RequiredArgsConstructor
public class SpringRedisIdempotencySnapshotCache
        implements RedisIdempotencySnapshotCache {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final RedisIdempotencySnapshotSerializer serializer;
    private final IdempotencyCacheMetrics metrics;

    @Override
    public Mono<CachedIdempotencySnapshot> getCompletedSnapshot(
            IdempotencyScope scope,
            IdempotencyKey idempotencyKey
    ) {
        var key = RedisIdempotencyKeyFormatter.completedSnapshotKey(
                scope,
                idempotencyKey
        );

        return redisTemplate.opsForValue()
                .get(key)
                .map(serializer::deserialize)
                .doOnNext(ignored -> metrics.recordRedisHit(scope.name()))
                .switchIfEmpty(Mono.defer(() -> {
                    metrics.recordRedisMiss(scope.name());
                    return Mono.empty();
                }));
    }

    @Override
    public Mono<Void> putCompletedSnapshot(
            IdempotencyScope scope,
            IdempotencyKey idempotencyKey,
            String requestFingerprint,
            String responseBodyJson,
            Duration ttl
    ) {
        var key = RedisIdempotencyKeyFormatter.completedSnapshotKey(
                scope,
                idempotencyKey
        );
        var snapshot = new CachedIdempotencySnapshot(
                requestFingerprint,
                responseBodyJson
        );

        return redisTemplate.opsForValue()
                .set(key, serializer.serialize(snapshot), ttl)
                .doOnError(ignored -> metrics.recordRedisWriteFailure(scope.name()))
                .then();
    }
}
