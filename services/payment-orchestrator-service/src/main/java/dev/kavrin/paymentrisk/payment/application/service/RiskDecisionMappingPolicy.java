package dev.kavrin.paymentrisk.payment.application.service;

import dev.kavrin.paymentrisk.payment.domain.model.PaymentRiskDecision;
import dev.kavrin.paymentrisk.payment.domain.model.RiskDecision;
import dev.kavrin.paymentrisk.risk.application.dto.RiskScoringOutcome;
import dev.kavrin.paymentrisk.risk.application.dto.RiskScoringResponse;
import dev.kavrin.paymentrisk.shared.api.error.DownstreamTimeoutException;
import dev.kavrin.paymentrisk.shared.api.error.DownstreamUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public final class RiskDecisionMappingPolicy {

    private final Clock clock;

    public PaymentRiskDecision map(RiskScoringResponse response) {
        Instant decidedAt = clock.instant();

        return switch (response.outcome()) {
            case APPROVED -> toPaymentRiskDecision(
                    RiskDecision.APPROVED,
                    response,
                    decidedAt
            );

            case DECLINED -> toPaymentRiskDecision(
                    RiskDecision.DECLINED,
                    response,
                    decidedAt
            );

            case REVIEW_REQUIRED -> toPaymentRiskDecision(
                    RiskDecision.DECLINED,
                    withExtraReason(response, "REVIEW_REQUIRED"),
                    decidedAt
            );

            case TIMEOUT -> throw new DownstreamTimeoutException(
                    "Risk service timed out"
            );

            case UNAVAILABLE -> throw new DownstreamUnavailableException(
                    "Risk service is unavailable"
            );
        };
    }

    private static PaymentRiskDecision toPaymentRiskDecision(
            RiskDecision decision,
            RiskScoringResponse response,
            Instant decidedAt
    ) {
        return new PaymentRiskDecision(
                decision,
                response.score(),
                response.reasonCodes(),
                response.ruleVersion(),
                decidedAt
        );
    }

    private static RiskScoringResponse withExtraReason(
            RiskScoringResponse response,
            String reasonCode
    ) {
        List<String> reasonCodes = new ArrayList<>(response.reasonCodes());

        if (!reasonCodes.contains(reasonCode)) {
            reasonCodes.add(reasonCode);
        }

        return new RiskScoringResponse(
                response.outcome(),
                response.score(),
                reasonCodes,
                response.ruleVersion()
        );
    }
}
