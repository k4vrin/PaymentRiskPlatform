package dev.kavrin.paymentrisk.idempotency.infrastructure.persistence;

import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKeyConflictException;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyScope;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyStatus;
import dev.kavrin.paymentrisk.payment.application.service.AuthorizePaymentResultSnapshotSerializer;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Objects;

@Component
public final class DatabaseIdempotencyResultStore {

    private final IdempotencyRecordEntityRepository repository;
    private final AuthorizePaymentResultSnapshotSerializer snapshotSerializer;

    public DatabaseIdempotencyResultStore(
            IdempotencyRecordEntityRepository repository,
            AuthorizePaymentResultSnapshotSerializer snapshotSerializer
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.snapshotSerializer = Objects.requireNonNull(snapshotSerializer, "snapshotSerializer must not be null");
    }

    public <T> Mono<T> findCompletedResult(
            IdempotencyScope scope,
            IdempotencyKey key,
            String requestFingerprint,
            Instant now,
            Class<T> responseType
    ) {
        Objects.requireNonNull(scope, "scope must not be null");
        Objects.requireNonNull(key, "key must not be null");
        String fingerprint = requireText(requestFingerprint, "requestFingerprint");
        Objects.requireNonNull(now, "now must not be null");
        Objects.requireNonNull(responseType, "responseType must not be null");

        return repository.findByScopeAndIdempotencyKey(scope.value(), key.value())
                .flatMap(entity -> resolveStoredResult(entity, fingerprint, now, responseType));
    }

    private <T> Mono<T> resolveStoredResult(
            IdempotencyRecordEntity entity,
            String requestFingerprint,
            Instant now,
            Class<T> responseType
    ) {
        if (isExpired(entity, now)) {
            return Mono.empty();
        }

        if (!Objects.equals(entity.getRequestFingerprint(), requestFingerprint)) {
            return Mono.error(new IdempotencyKeyConflictException());
        }

        if (!IdempotencyStatus.COMPLETED.name().equals(entity.getStatus())) {
            return Mono.empty();
        }

        return Mono.fromSupplier(() -> responseType.cast(
                snapshotSerializer.deserialize(entity.getResponseBodyJson(), responseType)
        ));
    }

    private static boolean isExpired(IdempotencyRecordEntity entity, Instant now) {
        Instant expiresAt = Objects.requireNonNull(entity.getExpiresAt(), "expiresAt must not be null");
        return !expiresAt.isAfter(now);
    }

    private static String requireText(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        return normalized;
    }
}
