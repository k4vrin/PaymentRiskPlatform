package dev.kavrin.paymentrisk.idempotency.domain;

import java.util.Arrays;

public enum IdempotencyScope {
    PAYMENT_AUTHORIZATION("payment_authorization");

    private final String value;

    IdempotencyScope(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static IdempotencyScope fromValue(String value) {
        return Arrays.stream(values())
                .filter(scope -> scope.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown idempotency scope: " + value));
    }
}
