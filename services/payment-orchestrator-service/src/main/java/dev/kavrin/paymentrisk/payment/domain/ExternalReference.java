package dev.kavrin.paymentrisk.payment.domain;

import java.util.Optional;

public record ExternalReference(String value) {

    public ExternalReference {
        value = RequiredText.require(value, "externalReference", 120);
    }

    public static ExternalReference of(String value) {
        return new ExternalReference(value);
    }

    public static Optional<ExternalReference> optional(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new ExternalReference(value));
    }
}
