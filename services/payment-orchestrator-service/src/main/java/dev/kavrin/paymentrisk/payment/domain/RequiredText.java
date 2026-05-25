package dev.kavrin.paymentrisk.payment.domain;

final class RequiredText {

    private RequiredText() {
    }

    static String require(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }

        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must be at most " + maxLength + " characters.");
        }

        return normalized;
    }
}
