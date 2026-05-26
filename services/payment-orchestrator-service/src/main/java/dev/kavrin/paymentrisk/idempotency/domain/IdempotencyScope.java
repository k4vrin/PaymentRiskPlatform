package dev.kavrin.paymentrisk.idempotency.domain;

public enum IdempotencyScope {
    PAYMENT_AUTHORIZATION("payment_authorization");

    private final String value;

    IdempotencyScope(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
