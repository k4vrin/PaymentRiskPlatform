package dev.kavrin.paymentrisk.idempotency.domain;

import java.util.Objects;
import java.util.regex.Pattern;

public record IdempotencyKey(String value) {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;
    private static final Pattern ALLOWED_PATTERN = Pattern.compile("^[A-Za-z0-9._:-]+$");

    public IdempotencyKey {
        Objects.requireNonNull(value, "Idempotency key is required");

        value = value.trim();

        if (value.isBlank()) {
            throw new IllegalArgumentException("Idempotency key is required");
        }

        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Idempotency key must be between 8 and 128 characters");
        }

        if (!ALLOWED_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Idempotency key may contain letters, numbers, dot, underscore, colon, and hyphen"
            );
        }
    }

    public static IdempotencyKey of(String value) {
        return new IdempotencyKey(value);
    }
}