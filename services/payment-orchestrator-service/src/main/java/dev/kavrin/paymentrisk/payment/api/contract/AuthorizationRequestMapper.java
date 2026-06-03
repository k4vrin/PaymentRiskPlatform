package dev.kavrin.paymentrisk.payment.api.contract;

import dev.kavrin.paymentrisk.payment.api.dto.AuthorizationRequest;
import dev.kavrin.paymentrisk.payment.application.command.AuthorizePaymentCommand;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class AuthorizationRequestMapper {

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