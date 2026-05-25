package dev.kavrin.paymentrisk.payment.application.command;

import java.util.Objects;

public record AuthorizePaymentCommand(
        String merchantId,
        String customerId,
        long amountMinor,
        String currency,
        String paymentMethodToken,
        String deviceFingerprint,
        String externalReference,
        String idempotencyKey,
        String correlationId
) {

    public AuthorizePaymentCommand {
        merchantId = requireText(merchantId, "merchantId");
        customerId = requireText(customerId, "customerId");

        if (amountMinor <= 0) {
            throw new IllegalArgumentException("amountMinor must be positive");
        }

        currency = requireText(currency, "currency");
        paymentMethodToken = requireText(paymentMethodToken, "paymentMethodToken");
        deviceFingerprint = requireText(deviceFingerprint, "deviceFingerprint");
        externalReference = normalizeOptionalText(externalReference);
        idempotencyKey = requireText(idempotencyKey, "idempotencyKey");
        correlationId = requireText(correlationId, "correlationId");
    }

    private static String requireText(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        return normalized;
    }

    private static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}