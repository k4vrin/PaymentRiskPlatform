package dev.kavrin.paymentrisk.risk.application.dto;

import java.util.List;
import java.util.Objects;

public record RiskScoringResponse(
        RiskScoringOutcome outcome,
        int score,
        List<String> reasonCodes,
        String ruleVersion
) {

    public RiskScoringResponse {
        Objects.requireNonNull(outcome, "outcome must not be null");
        reasonCodes = Objects.requireNonNull(reasonCodes, "reasonCodes must not be null")
                .stream()
                .map(reasonCode -> requiredText(reasonCode, "reasonCode"))
                .toList();
        ruleVersion = requiredText(ruleVersion, "ruleVersion");

        if (score < 0) {
            throw new IllegalArgumentException("score must not be negative");
        }
    }

    public static RiskScoringResponse approved(
            int score,
            List<String> reasonCodes,
            String ruleVersion
    ) {
        return new RiskScoringResponse(
                RiskScoringOutcome.APPROVED,
                score,
                List.copyOf(reasonCodes),
                ruleVersion
        );
    }

    public static RiskScoringResponse declined(
            int score,
            List<String> reasonCodes,
            String ruleVersion
    ) {
        return new RiskScoringResponse(
                RiskScoringOutcome.DECLINED,
                score,
                List.copyOf(reasonCodes),
                ruleVersion
        );
    }

    public static RiskScoringResponse reviewRequired(
            int score,
            List<String> reasonCodes,
            String ruleVersion
    ) {
        return new RiskScoringResponse(
                RiskScoringOutcome.REVIEW_REQUIRED,
                score,
                List.copyOf(reasonCodes),
                ruleVersion
        );
    }

    public static RiskScoringResponse timeout() {
        return new RiskScoringResponse(
                RiskScoringOutcome.TIMEOUT,
                0,
                List.of("RISK_SERVICE_TIMEOUT"),
                "unavailable"
        );
    }

    public static RiskScoringResponse unavailable() {
        return new RiskScoringResponse(
                RiskScoringOutcome.UNAVAILABLE,
                0,
                List.of("DOWNSTREAM_UNAVAILABLE"),
                "unavailable"
        );
    }

    private static String requiredText(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        return normalized;
    }
}
