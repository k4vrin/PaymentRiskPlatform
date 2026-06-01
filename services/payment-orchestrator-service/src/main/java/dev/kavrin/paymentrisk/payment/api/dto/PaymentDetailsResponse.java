package dev.kavrin.paymentrisk.payment.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Non-sensitive public payment detail response.")
public record PaymentDetailsResponse(
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
