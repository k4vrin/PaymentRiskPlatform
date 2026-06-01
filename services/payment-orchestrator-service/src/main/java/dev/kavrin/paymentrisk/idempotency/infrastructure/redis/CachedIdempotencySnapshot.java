package dev.kavrin.paymentrisk.idempotency.infrastructure.redis;

public record CachedIdempotencySnapshot(
        String requestFingerprint,
        String responseBodyJson
) {
}
