package dev.kavrin.paymentrisk.idempotency.application;

import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyScope;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

public interface IdempotencyResultStore {

    Duration DEFAULT_RESULT_TTL = Duration.ofHours(24);

    <T> T getOrCreateCompletedResult(
            IdempotencyScope scope,
            IdempotencyKey key,
            String requestFingerprint,
            Instant now,
            Supplier<T> responseSupplier
    );

    <T> T getOrCreateCompletedResult(
            IdempotencyScope scope,
            IdempotencyKey key,
            String requestFingerprint,
            Instant now,
            Duration resultTtl,
            Supplier<T> responseSupplier
    );
}
