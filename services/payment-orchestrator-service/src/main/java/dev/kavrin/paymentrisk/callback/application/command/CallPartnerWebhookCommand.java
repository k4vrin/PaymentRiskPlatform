package dev.kavrin.paymentrisk.callback.application.command;

import dev.kavrin.paymentrisk.callback.domain.CallbackType;

/**
 * Command instructing a worker to call a partner webhook.
 */
public record CallPartnerWebhookCommand(
        String paymentId,
        String merchantId,
        String targetUrl,
        CallbackType callbackType,
        int attempt,
        String correlationId
) {

    public CallPartnerWebhookCommand {
        paymentId = requireText(paymentId, "paymentId");
        merchantId = requireText(merchantId, "merchantId");
        targetUrl = requireText(targetUrl, "targetUrl");
        correlationId = requireText(correlationId, "correlationId");

        if (callbackType == null) {
            throw new IllegalArgumentException("callbackType is required");
        }

        if (attempt < 0) {
            throw new IllegalArgumentException("attempt must not be negative");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }

        return value.trim();
    }
}
