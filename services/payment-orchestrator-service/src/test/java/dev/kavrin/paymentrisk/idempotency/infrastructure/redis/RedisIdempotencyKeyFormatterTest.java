package dev.kavrin.paymentrisk.idempotency.infrastructure.redis;

import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RedisIdempotencyKeyFormatterTest {

    @Test
    void formatsCompletedSnapshotKey() {
        String key = RedisIdempotencyKeyFormatter.completedSnapshotKey(
                IdempotencyScope.PAYMENT_AUTHORIZATION,
                IdempotencyKey.of("idem_123")
        );

        assertThat(key)
                .isEqualTo("idempotency:payment_authorization:idem_123:completed-response");
    }
}
