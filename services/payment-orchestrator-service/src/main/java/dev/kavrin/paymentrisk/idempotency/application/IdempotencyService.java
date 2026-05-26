package dev.kavrin.paymentrisk.idempotency.application;

import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKeyConflictException;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyScope;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@Component
public final class IdempotencyService {

    public static final Duration DEFAULT_RESULT_TTL = Duration.ofHours(24);

    private final Map<ScopedIdempotencyKey, StoredIdempotencyResult> results;

    public IdempotencyService() {
        this(new ConcurrentHashMap<>());
    }

    IdempotencyService(Map<ScopedIdempotencyKey, StoredIdempotencyResult> results) {
        this.results = Objects.requireNonNull(results, "results must not be null");
    }

    public <T> T getOrCreateCompletedResult(
            IdempotencyScope scope,
            IdempotencyKey key,
            String requestFingerprint,
            Instant now,
            Supplier<T> responseSupplier
    ) {
        return getOrCreateCompletedResult(scope, key, requestFingerprint, now, DEFAULT_RESULT_TTL, responseSupplier);
    }

    public <T> T getOrCreateCompletedResult(
            IdempotencyScope scope,
            IdempotencyKey key,
            String requestFingerprint,
            Instant now,
            Duration resultTtl,
            Supplier<T> responseSupplier
    ) {
        Objects.requireNonNull(scope, "scope must not be null");
        Objects.requireNonNull(key, "key must not be null");
        String fingerprint = requireText(requestFingerprint, "requestFingerprint");
        Objects.requireNonNull(now, "now must not be null");
        Objects.requireNonNull(resultTtl, "resultTtl must not be null");
        Objects.requireNonNull(responseSupplier, "responseSupplier must not be null");

        if (resultTtl.isZero() || resultTtl.isNegative()) {
            throw new IllegalArgumentException("resultTtl must be positive");
        }

        ScopedIdempotencyKey scopedKey = new ScopedIdempotencyKey(scope, key);
        AtomicReference<StoredIdempotencyResult> selectedResult = new AtomicReference<>();

        results.compute(scopedKey, (ignored, existing) -> {
            if (existing != null && !existing.isExpiredAt(now)) {
                if (!existing.requestFingerprint().equals(fingerprint)) {
                    throw new IdempotencyKeyConflictException();
                }

                selectedResult.set(existing);
                return existing;
            }

            T response = Objects.requireNonNull(responseSupplier.get(), "responseSupplier must return a response");
            StoredIdempotencyResult created = new StoredIdempotencyResult(
                    fingerprint,
                    IdempotencyStatus.COMPLETED,
                    200,
                    response,
                    now.plus(resultTtl),
                    now,
                    now
            );
            selectedResult.set(created);
            return created;
        });

        return castSnapshot(selectedResult.get().responseSnapshot());
    }

    @SuppressWarnings("unchecked")
    private static <T> T castSnapshot(Object responseSnapshot) {
        return (T) responseSnapshot;
    }

    private static String requireText(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        return normalized;
    }
}
