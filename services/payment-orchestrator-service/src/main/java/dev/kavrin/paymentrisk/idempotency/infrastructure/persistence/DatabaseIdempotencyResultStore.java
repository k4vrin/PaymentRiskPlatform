package dev.kavrin.paymentrisk.idempotency.infrastructure.persistence;

import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKeyConflictException;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyScope;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyStatus;
import dev.kavrin.paymentrisk.payment.application.service.AuthorizePaymentResultSnapshotSerializer;
import dev.kavrin.paymentrisk.shared.id.PlatformIdGeneratorFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Objects;

@Component
public final class DatabaseIdempotencyResultStore implements DatabaseIdempotencyResultOperations {

    private final IdempotencyRecordEntityRepository repository;
    private final R2dbcEntityTemplate entityTemplate;
    private final AuthorizePaymentResultSnapshotSerializer snapshotSerializer;
    private final PlatformIdGeneratorFactory idGenerator;

    public DatabaseIdempotencyResultStore(
            IdempotencyRecordEntityRepository repository,
            R2dbcEntityTemplate entityTemplate,
            AuthorizePaymentResultSnapshotSerializer snapshotSerializer,
            PlatformIdGeneratorFactory idGenerator
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.entityTemplate = Objects.requireNonNull(entityTemplate, "entityTemplate must not be null");
        this.snapshotSerializer = Objects.requireNonNull(snapshotSerializer, "snapshotSerializer must not be null");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
    }

    @Override
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

    @Override
    public Mono<Void> insertStarted(
            IdempotencyScope scope,
            IdempotencyKey key,
            String requestFingerprint,
            Instant now,
            Instant expiresAt
    ) {
        Objects.requireNonNull(scope, "scope must not be null");
        Objects.requireNonNull(key, "key must not be null");
        String fingerprint = requireText(requestFingerprint, "requestFingerprint");
        Objects.requireNonNull(now, "now must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");

        IdempotencyRecordEntity entity = new IdempotencyRecordEntity();
        entity.setIdempotencyRecordId(idGenerator.idempotencyRecordId());
        entity.setScope(scope.value());
        entity.setIdempotencyKey(key.value());
        entity.setRequestFingerprint(fingerprint);
        entity.setStatus(IdempotencyStatus.STARTED.name());
        entity.setExpiresAt(expiresAt);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        return entityTemplate.insert(IdempotencyRecordEntity.class)
                .using(entity)
                .then()
                .onErrorMap(this::isDuplicateKeyError, exception -> new IdempotencyKeyConflictException());
    }

    @Override
    public <T> Mono<Void> markCompleted(
            IdempotencyScope scope,
            IdempotencyKey key,
            String requestFingerprint,
            T response,
            int responseStatus,
            Instant now
    ) {
        Objects.requireNonNull(scope, "scope must not be null");
        Objects.requireNonNull(key, "key must not be null");
        String fingerprint = requireText(requestFingerprint, "requestFingerprint");
        Objects.requireNonNull(response, "response must not be null");
        Objects.requireNonNull(now, "now must not be null");

        return repository.findByScopeAndIdempotencyKey(scope.value(), key.value())
                .switchIfEmpty(Mono.error(new IllegalStateException("Idempotency STARTED record was not found")))
                .flatMap(entity -> {
                    verifySameFingerprint(entity, fingerprint);

                    entity.setStatus(IdempotencyStatus.COMPLETED.name());
                    entity.setResponseStatus(responseStatus);
                    entity.setResponseBodyJson(snapshotSerializer.serialize(response));
                    entity.setUpdatedAt(now);

                    return repository.save(entity).then();
                });
    }

    @Override
    public Mono<Void> markFailedAndExpire(
            IdempotencyScope scope,
            IdempotencyKey key,
            String requestFingerprint,
            Instant now
    ) {
        Objects.requireNonNull(scope, "scope must not be null");
        Objects.requireNonNull(key, "key must not be null");
        String fingerprint = requireText(requestFingerprint, "requestFingerprint");
        Objects.requireNonNull(now, "now must not be null");

        return repository.findByScopeAndIdempotencyKey(scope.value(), key.value())
                .switchIfEmpty(Mono.empty())
                .flatMap(entity -> {
                    verifySameFingerprint(entity, fingerprint);

                    entity.setStatus(IdempotencyStatus.FAILED.name());
                    entity.setExpiresAt(now);
                    entity.setUpdatedAt(now);

                    return repository.save(entity).then();
                });
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

    private void verifySameFingerprint(
            IdempotencyRecordEntity entity,
            String requestFingerprint
    ) {
        if (!Objects.equals(entity.getRequestFingerprint(), requestFingerprint)) {
            throw new IdempotencyKeyConflictException();
        }
    }

    private boolean isDuplicateKeyError(Throwable exception) {
        return exception instanceof DataIntegrityViolationException;
    }
}
