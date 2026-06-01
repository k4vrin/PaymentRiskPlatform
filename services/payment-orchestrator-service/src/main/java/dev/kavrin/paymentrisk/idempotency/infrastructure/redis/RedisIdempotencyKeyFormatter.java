package dev.kavrin.paymentrisk.idempotency.infrastructure.redis;

import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyScope;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class RedisIdempotencyKeyFormatter {

    public static String completedSnapshotKey(
            IdempotencyScope scope,
            IdempotencyKey idempotencyKey
    ) {
        return "idempotency:%s:%s:completed-response"
                .formatted(scope.value(), idempotencyKey.value());
    }
}
