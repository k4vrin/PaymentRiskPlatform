package dev.kavrin.paymentrisk.payment.api.contract;

import dev.kavrin.paymentrisk.payment.api.dto.AuthorizationResponse;
import dev.kavrin.paymentrisk.payment.application.command.AuthorizePaymentResult;

final class AuthorizationResponseMapper {

    private AuthorizationResponseMapper() {
    }

    static AuthorizationResponse toResponse(AuthorizePaymentResult result) {
        return new AuthorizationResponse(
                result.paymentId(),
                result.status(),
                result.authorizationCode(),
                result.riskDecision(),
                result.reasonCodes(),
                result.correlationId(),
                result.riskScore(),
                result.ruleVersion(),
                result.createdAt()
        );
    }
}
