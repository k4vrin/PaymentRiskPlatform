package dev.kavrin.paymentrisk.payment.application.command;

import java.time.Instant;

public record ReversePaymentResult(
        String paymentId,
        String reversalId,
        String status,
        String reason,
        String correlationId,
        Instant reversedAt
) {
}
