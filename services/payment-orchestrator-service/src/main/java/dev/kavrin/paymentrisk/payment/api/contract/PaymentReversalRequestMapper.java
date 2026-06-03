package dev.kavrin.paymentrisk.payment.api.contract;

import dev.kavrin.paymentrisk.payment.api.dto.ReversePaymentRequest;
import dev.kavrin.paymentrisk.payment.application.command.ReversePaymentCommand;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class PaymentReversalRequestMapper {

    static ReversePaymentCommand toCommand(
            String paymentId,
            ReversePaymentRequest request,
            String correlationId
    ) {
        return new ReversePaymentCommand(
                paymentId,
                request.idempotencyKey(),
                request.reason(),
                correlationId
        );
    }
}
