package dev.kavrin.paymentrisk.idempotency.infrastructure.persistence;

import dev.kavrin.paymentrisk.idempotency.application.StoredIdempotencyResult;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyScope;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyStatus;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public final class IdempotencyRecordMapper {

    public IdempotencyRecordEntity toEntity(
            String idempotencyRecordId,
            IdempotencyScope scope,
            IdempotencyKey key,
            StoredIdempotencyResult result,
            String paymentId,
            String responseBodyJson
    ) {
        Objects.requireNonNull(scope, "scope must not be null");
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(result, "result must not be null");

        return IdempotencyRecordEntity.builder()
                .idempotencyRecordId(idempotencyRecordId)
                .scope(scope.value())
                .idempotencyKey(key.value())
                .requestFingerprint(result.requestFingerprint())
                .paymentId(paymentId)
                .status(result.status().name())
                .responseStatus(result.responseStatus())
                .responseBodyJson(responseBodyJson)
                .expiresAt(result.expiresAt())
                .createdAt(result.createdAt())
                .updatedAt(result.updatedAt())
                .build();
    }

    public StoredIdempotencyResult toStoredResult(
            IdempotencyRecordEntity entity,
            Object responseSnapshot
    ) {
        Objects.requireNonNull(entity, "entity must not be null");

        return new StoredIdempotencyResult(
                entity.getRequestFingerprint(),
                IdempotencyStatus.valueOf(entity.getStatus()),
                entity.getResponseStatus(),
                responseSnapshot,
                entity.getExpiresAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public IdempotencyScope toScope(IdempotencyRecordEntity entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        return IdempotencyScope.fromValue(entity.getScope());
    }

    public IdempotencyKey toKey(IdempotencyRecordEntity entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        return IdempotencyKey.of(entity.getIdempotencyKey());
    }
}
