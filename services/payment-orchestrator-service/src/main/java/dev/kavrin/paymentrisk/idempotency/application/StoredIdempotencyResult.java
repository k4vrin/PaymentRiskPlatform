package dev.kavrin.paymentrisk.idempotency.application;

import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyStatus;

import java.time.Instant;
import java.util.Objects;

record StoredIdempotencyResult(
        String requestFingerprint,
        IdempotencyStatus status,
        int responseStatus,
        Object responseSnapshot,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt
) {

    StoredIdempotencyResult {
        requestFingerprint = requireText(requestFingerprint, "requestFingerprint");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(responseSnapshot, "responseSnapshot must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    boolean isExpiredAt(Instant now) {
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
