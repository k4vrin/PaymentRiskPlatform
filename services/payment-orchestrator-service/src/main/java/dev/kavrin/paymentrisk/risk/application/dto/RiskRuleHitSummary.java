package dev.kavrin.paymentrisk.risk.application.dto;

import java.util.Objects;

public record RiskRuleHitSummary(
        String ruleId,
        String reasonCode,
        int scoreDelta,
        String message
) {

    public RiskRuleHitSummary {
        ruleId = requiredText(ruleId, "ruleId");
        reasonCode = requiredText(reasonCode, "reasonCode");
        message = requiredText(message, "message");
    }

    private static String requiredText(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        return normalized;
    }
}
