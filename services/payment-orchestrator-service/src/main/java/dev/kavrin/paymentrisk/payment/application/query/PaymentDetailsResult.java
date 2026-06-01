package dev.kavrin.paymentrisk.payment.application.query;

import java.time.Instant;
import java.util.List;

public record PaymentDetailsResult(
        String paymentId,
        String merchantId,
        String customerId,
        long amountMinor,
        String currency,
        String status,
        String externalReference,
        AuthorizationDetails authorization,
        RiskDetails risk,
        ReversalDetails reversal,
        Instant createdAt,
        Instant updatedAt
) {

    public record AuthorizationDetails(
            String status,
            String authorizationCode,
            Instant requestedAt,
            Instant riskPendingAt,
            Instant authorizedAt,
            Instant declinedAt,
            Instant failedAt
    ) {
    }

    public record RiskDetails(
            String decision,
            int score,
            List<String> reasonCodes,
            String ruleVersion,
            Instant decidedAt
    ) {

        public RiskDetails {
            reasonCodes = List.copyOf(reasonCodes);
        }
    }

    public record ReversalDetails(
            String reversalId,
            String status,
            String reason,
            Instant requestedAt,
            Instant reversedAt
    ) {
    }
}
