package dev.kavrin.paymentrisk.payment.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Public JSON response body for a payment reversal.")
public record PaymentReversalResponse(
        String paymentId,
        String reversalId,
        String status,
        String reason,
        String correlationId,
        Instant reversedAt
) {
}
