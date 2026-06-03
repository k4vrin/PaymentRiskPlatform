package dev.kavrin.paymentrisk.payment.application.outbox;

import java.time.Instant;

public record PaymentReversedPayload(
        String paymentId,
        String reversalId,
        String merchantId,
        String customerId,
        Long amountMinor,
        String currency,
        String reason,
        Instant reversedAt
) {
}