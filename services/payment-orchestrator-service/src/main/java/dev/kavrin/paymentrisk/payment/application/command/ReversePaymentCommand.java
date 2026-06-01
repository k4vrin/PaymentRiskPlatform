package dev.kavrin.paymentrisk.payment.application.command;

public record ReversePaymentCommand(
        String paymentId,
        String idempotencyKey,
        String reason,
        String correlationId
) {
}
