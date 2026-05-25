package dev.kavrin.paymentrisk.payment.api.contract;

import dev.kavrin.paymentrisk.payment.api.dto.AuthorizationRequest;
import dev.kavrin.paymentrisk.payment.application.command.AuthorizePaymentCommand;

final class AuthorizationRequestMapper {

    private AuthorizationRequestMapper() {
    }

    static AuthorizePaymentCommand toCommand(
            AuthorizationRequest request,
            String correlationId
    ) {
        return new AuthorizePaymentCommand(
                request.merchantId(),
                request.customerId(),
                request.amountMinor(),
                request.currency(),
                request.paymentMethodToken(),
                request.deviceFingerprint(),
                request.externalReference(),
                request.idempotencyKey(),
                correlationId
        );
    }
}