package dev.kavrin.paymentrisk.idempotency.application;

import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyScope;

import java.util.Objects;

record ScopedIdempotencyKey(IdempotencyScope scope, IdempotencyKey key) {

    ScopedIdempotencyKey {
        Objects.requireNonNull(scope, "scope must not be null");
        Objects.requireNonNull(key, "key must not be null");
    }
}
