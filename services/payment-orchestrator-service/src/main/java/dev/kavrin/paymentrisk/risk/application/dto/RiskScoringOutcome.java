package dev.kavrin.paymentrisk.risk.application.dto;

public enum RiskScoringOutcome {
    APPROVED,
    DECLINED,
    REVIEW_REQUIRED,
    TIMEOUT,
    UNAVAILABLE
}