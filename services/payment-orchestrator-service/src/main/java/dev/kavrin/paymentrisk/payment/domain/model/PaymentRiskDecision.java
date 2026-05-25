package dev.kavrin.paymentrisk.payment.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record PaymentRiskDecision(
        RiskDecision decision,
        int score,
        List<String> reasonCodes,
        String ruleVersion,
        Instant decidedAt
) {

    public PaymentRiskDecision {
        decision = Objects.requireNonNull(decision);
        reasonCodes = List.copyOf(Objects.requireNonNullElse(reasonCodes, List.of()));
        ruleVersion = Objects.requireNonNull(ruleVersion);
        decidedAt = Objects.requireNonNull(decidedAt);

        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("Risk score must be between 0 and 100");
        }
    }

    public boolean isApproved() {
        return decision == RiskDecision.APPROVED;
    }

    public boolean isDeclined() {
        return decision == RiskDecision.DECLINED;
    }
}