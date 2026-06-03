package dev.kavrin.paymentrisk.payment.api.contract;

import dev.kavrin.paymentrisk.payment.api.dto.PaymentDetailsResponse;
import dev.kavrin.paymentrisk.payment.application.query.PaymentDetailsResult;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class PaymentDetailsResponseMapper {

    static PaymentDetailsResponse toResponse(PaymentDetailsResult result) {
        return new PaymentDetailsResponse(
                result.paymentId(),
                result.merchantId(),
                result.customerId(),
                result.amountMinor(),
                result.currency(),
                result.status(),
                result.externalReference(),
                toAuthorizationResponse(result.authorization()),
                toRiskResponse(result.risk()),
                toReversalResponse(result.reversal()),
                result.createdAt(),
                result.updatedAt()
        );
    }

    private static PaymentDetailsResponse.AuthorizationDetails toAuthorizationResponse(
            PaymentDetailsResult.AuthorizationDetails details
    ) {
        if (details == null) {
            return null;
        }

        return new PaymentDetailsResponse.AuthorizationDetails(
                details.status(),
                details.authorizationCode(),
                details.requestedAt(),
                details.riskPendingAt(),
                details.authorizedAt(),
                details.declinedAt(),
                details.failedAt()
        );
    }

    private static PaymentDetailsResponse.RiskDetails toRiskResponse(
            PaymentDetailsResult.RiskDetails details
    ) {
        if (details == null) {
            return null;
        }

        return new PaymentDetailsResponse.RiskDetails(
                details.decision(),
                details.score(),
                details.reasonCodes(),
                details.ruleVersion(),
                details.decidedAt()
        );
    }

    private static PaymentDetailsResponse.ReversalDetails toReversalResponse(
            PaymentDetailsResult.ReversalDetails details
    ) {
        if (details == null) {
            return null;
        }

        return new PaymentDetailsResponse.ReversalDetails(
                details.reversalId(),
                details.status(),
                details.reason(),
                details.requestedAt(),
                details.reversedAt()
        );
    }
}
