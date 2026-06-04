package dev.kavrin.paymentrisk.ops.application.payment;

import dev.kavrin.paymentrisk.payment.domain.model.PaymentStatus;

import java.time.Instant;
import java.util.Optional;

public record OpsPaymentSearchItem(
        String paymentId,
        String merchantId,
        String customerId,
        long amountMinor,
        String currency,
        PaymentStatus status,
        Optional<String> externalReference,
        Optional<AuthorizationSummary> authorization,
        Optional<RiskSummary> risk,
        Optional<ReversalSummary> reversal,
        Instant createdAt,
        Instant updatedAt
) {
    public OpsPaymentSearchItem {
        externalReference = normalize(externalReference);
        authorization = normalize(authorization);
        risk = normalize(risk);
        reversal = normalize(reversal);
    }

    private static <T> Optional<T> normalize(Optional<T> value) {
        return value == null ? Optional.empty() : value;
    }

    public record AuthorizationSummary(
            String authorizationStatus,
            Optional<String> authorizationCode,
            Optional<Instant> authorizedAt
    ) {
        public AuthorizationSummary {
            authorizationCode = normalize(authorizationCode);
            authorizedAt = normalize(authorizedAt);
        }
    }

    public record RiskSummary(
            String decision,
            int score,
            String ruleVersion,
            Instant decidedAt
    ) {
    }

    public record ReversalSummary(
            String reversalId,
            String status,
            String reason,
            Instant reversedAt
    ) {
    }
}
