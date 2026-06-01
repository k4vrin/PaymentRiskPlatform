package dev.kavrin.paymentrisk.idempotency.infrastructure.redis;

import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyScope;
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
                .map(serializer::deserialize);
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
                .then();
    }
}
