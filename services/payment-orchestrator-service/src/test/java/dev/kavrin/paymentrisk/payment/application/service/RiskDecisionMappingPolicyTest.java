package dev.kavrin.paymentrisk.payment.application.service;

import dev.kavrin.paymentrisk.payment.domain.model.RiskDecision;
import dev.kavrin.paymentrisk.risk.application.dto.RiskScoringResponse;
import dev.kavrin.paymentrisk.shared.api.error.DownstreamTimeoutException;
import dev.kavrin.paymentrisk.shared.api.error.DownstreamUnavailableException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RiskDecisionMappingPolicyTest {

    private static final Instant NOW = Instant.parse("2026-05-25T10:15:30Z");

    private final RiskDecisionMappingPolicy policy =
            new RiskDecisionMappingPolicy(Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void mapsApprovedRiskResponseToPaymentRiskDecisionUsingInjectedClock() {
        var decision = policy.map(RiskScoringResponse.approved(
                12,
                List.of("LOW_RISK_PAYMENT"),
                "risk-rules-v1"
        ));

        assertThat(decision.decision()).isEqualTo(RiskDecision.APPROVED);
        assertThat(decision.score()).isEqualTo(12);
        assertThat(decision.reasonCodes()).containsExactly("LOW_RISK_PAYMENT");
        assertThat(decision.ruleVersion()).isEqualTo("risk-rules-v1");
        assertThat(decision.decidedAt()).isEqualTo(NOW);
    }

    @Test
    void mapsDeclinedRiskResponseToPaymentRiskDecision() {
        var decision = policy.map(RiskScoringResponse.declined(
                95,
                List.of("HIGH_AMOUNT"),
                "risk-rules-v1"
        ));

        assertThat(decision.decision()).isEqualTo(RiskDecision.DECLINED);
        assertThat(decision.score()).isEqualTo(95);
        assertThat(decision.reasonCodes()).containsExactly("HIGH_AMOUNT");
    }

    @Test
    void mapsReviewRequiredRiskResponseToDeclinedDecisionWithReviewReason() {
        var decision = policy.map(RiskScoringResponse.reviewRequired(
                61,
                List.of("REPEATED_DEVICE"),
                "risk-rules-v1"
        ));

        assertThat(decision.decision()).isEqualTo(RiskDecision.DECLINED);
        assertThat(decision.reasonCodes()).containsExactly("REPEATED_DEVICE", "REVIEW_REQUIRED");
    }

    @Test
    void timeoutAndUnavailableRiskResponsesThrowStableDownstreamErrors() {
        assertThatThrownBy(() -> policy.map(RiskScoringResponse.timeout()))
                .isInstanceOf(DownstreamTimeoutException.class)
                .hasMessage("Risk service timed out");

        assertThatThrownBy(() -> policy.map(RiskScoringResponse.unavailable()))
                .isInstanceOf(DownstreamUnavailableException.class)
                .hasMessage("Risk service is unavailable");
    }
}
