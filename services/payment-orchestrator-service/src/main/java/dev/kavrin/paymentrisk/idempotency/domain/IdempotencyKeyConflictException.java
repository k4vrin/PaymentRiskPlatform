package dev.kavrin.paymentrisk.idempotency.domain;

public final class IdempotencyKeyConflictException extends RuntimeException {

    public IdempotencyKeyConflictException() {
        super("Idempotency key was already used for a different request");
    }
}
