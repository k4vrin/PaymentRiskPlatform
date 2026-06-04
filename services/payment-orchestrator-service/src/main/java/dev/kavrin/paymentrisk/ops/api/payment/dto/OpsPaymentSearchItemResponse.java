package dev.kavrin.paymentrisk.ops.api.payment.dto;

import java.time.Instant;

public record OpsPaymentSearchItemResponse(
        String paymentId,
        String merchantId,
        String customerId,
        long amountMinor,
        String currency,
        String status,
        String externalReference,
        AuthorizationSummaryResponse authorization,
        RiskSummaryResponse risk,
        ReversalSummaryResponse reversal,
        Instant createdAt,
        Instant updatedAt
) {
    public record AuthorizationSummaryResponse(
            String authorizationStatus,
            String authorizationCode,
            Instant authorizedAt
    ) {
    }

    public record RiskSummaryResponse(
            String decision,
            int score,
            String ruleVersion,
            Instant decidedAt
    ) {
    }

    public record ReversalSummaryResponse(
            String reversalId,
            String status,
            String reason,
            Instant reversedAt
    ) {
    }
}