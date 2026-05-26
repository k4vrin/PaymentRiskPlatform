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

        return IdempotencyRecordRow.builder()
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
