package dev.kavrin.paymentrisk.idempotency.infrastructure.persistence;

import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyScope;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface DatabaseIdempotencyResultOperations {

    <T> Mono<T> findCompletedResult(
            IdempotencyScope scope,
            IdempotencyKey key,
            String requestFingerprint,
            Instant now,
            Class<T> responseType
    );

    Mono<Void> insertStarted(
            IdempotencyScope scope,
            IdempotencyKey key,
            String requestFingerprint,
            Instant now,
            Instant expiresAt
    );

    <T> Mono<Void> markCompleted(
            IdempotencyScope scope,
            IdempotencyKey key,
            String requestFingerprint,
            T response,
            int responseStatus,
            Instant now
    );

    Mono<Void> markFailedAndExpire(
            IdempotencyScope scope,
            IdempotencyKey key,
            String requestFingerprint,
            Instant now
    );
}
