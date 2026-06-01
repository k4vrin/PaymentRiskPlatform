package dev.kavrin.paymentrisk.risk.application.dto;

import java.util.Objects;

public record RiskScoringRequest(
        String paymentId,
        long amountMinor,
        String currency,
        String merchantId,
        String customerId,
        String deviceFingerprint,
        String correlationId
) {

    public RiskScoringRequest {
        paymentId = requiredText(paymentId, "paymentId");
        currency = requiredText(currency, "currency");
        merchantId = requiredText(merchantId, "merchantId");
        customerId = requiredText(customerId, "customerId");
        deviceFingerprint = requiredText(deviceFingerprint, "deviceFingerprint");
        correlationId = requiredText(correlationId, "correlationId");

        if (amountMinor <= 0) {
            throw new IllegalArgumentException("amountMinor must be positive");
        }
    }

    private static String requiredText(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        return normalized;
    }
}
