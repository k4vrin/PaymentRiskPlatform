package dev.kavrin.paymentrisk.idempotency.domain;

public enum IdempotencyStatus {
    STARTED,
    FAILED,
    COMPLETED
}
