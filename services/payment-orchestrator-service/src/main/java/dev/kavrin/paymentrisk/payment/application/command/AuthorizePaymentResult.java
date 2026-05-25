package dev.kavrin.paymentrisk.payment.application.command;

import java.time.Instant;
import java.util.List;

public record AuthorizePaymentResult(
        String paymentId,
        String status,
        String authorizationCode,
        String riskDecision,
        List<String> reasonCodes,
        String correlationId,
        Integer riskScore,
        String ruleVersion,
        Instant createdAt
) {

    public AuthorizePaymentResult {
        reasonCodes = List.copyOf(reasonCodes);
    }
}
