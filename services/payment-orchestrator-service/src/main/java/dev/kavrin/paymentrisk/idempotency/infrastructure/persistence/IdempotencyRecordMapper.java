package dev.kavrin.paymentrisk.idempotency.infrastructure.persistence;

import dev.kavrin.paymentrisk.idempotency.application.StoredIdempotencyResult;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyScope;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyStatus;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.IdempotencyRecordRow;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public final class IdempotencyRecordMapper {

    public IdempotencyRecordRow toRow(
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

        return new IdempotencyRecordRow(
                idempotencyRecordId,
                scope.value(),
                key.value(),
                result.requestFingerprint(),
                paymentId,
                result.status().name(),
                result.responseStatus(),
                responseBodyJson,
                result.expiresAt(),
                result.createdAt(),
                result.updatedAt()
        );
    }

    public StoredIdempotencyResult toStoredResult(
            IdempotencyRecordRow row,
            Object responseSnapshot
    ) {
        Objects.requireNonNull(row, "row must not be null");

        return new StoredIdempotencyResult(
                row.getRequestFingerprint(),
                IdempotencyStatus.valueOf(row.getStatus()),
                row.getResponseStatus(),
                responseSnapshot,
                row.getExpiresAt(),
                row.getCreatedAt(),
                row.getUpdatedAt()
        );
    }

    public IdempotencyScope toScope(IdempotencyRecordRow row) {
        Objects.requireNonNull(row, "row must not be null");
        return IdempotencyScope.fromValue(row.getScope());
    }

    public IdempotencyKey toKey(IdempotencyRecordRow row) {
        Objects.requireNonNull(row, "row must not be null");
        return IdempotencyKey.of(row.getIdempotencyKey());
    }
}
