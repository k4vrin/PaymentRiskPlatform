package dev.kavrin.paymentrisk.idempotency.infrastructure.redis;

import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyScope;
import reactor.core.publisher.Mono;

import java.time.Duration;

public interface RedisIdempotencySnapshotCache {

    Mono<CachedIdempotencySnapshot> getCompletedSnapshot(
            IdempotencyScope scope,
            IdempotencyKey idempotencyKey
    );

    Mono<Void> putCompletedSnapshot(
            IdempotencyScope scope,
            IdempotencyKey idempotencyKey,
            String requestFingerprint,
            String responseBodyJson,
            Duration ttl
    );
}
