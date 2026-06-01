package dev.kavrin.paymentrisk.idempotency.infrastructure.persistence;

import java.time.Instant;
import java.util.Objects;

public record CompletedIdempotencyResult<T>(
        T response,
        Instant expiresAt
) {

    public CompletedIdempotencyResult {
        Objects.requireNonNull(response, "response must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    }
}
