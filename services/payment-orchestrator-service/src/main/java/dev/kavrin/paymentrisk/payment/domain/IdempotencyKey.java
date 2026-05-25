package dev.kavrin.paymentrisk.payment.domain;

import java.util.regex.Pattern;

public record IdempotencyKey(String value) {

    private static final int MIN_LENGTH = 16;
    private static final int MAX_LENGTH = 128;
    private static final Pattern ALLOWED = Pattern.compile("[A-Za-z0-9._:-]+");

    public IdempotencyKey {
        value = RequiredText.require(value, "idempotencyKey", MAX_LENGTH);
        if (value.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("idempotencyKey must be at least " + MIN_LENGTH + " characters.");
        }
        if (!ALLOWED.matcher(value).matches()) {
            throw new IllegalArgumentException("idempotencyKey contains unsupported characters.");
        }
    }

    public static IdempotencyKey of(String value) {
        return new IdempotencyKey(value);
    }
}
