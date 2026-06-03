package dev.kavrin.paymentrisk.payment.api.contract;

import dev.kavrin.paymentrisk.payment.api.dto.PaymentReversalResponse;
import dev.kavrin.paymentrisk.payment.application.command.ReversePaymentResult;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class PaymentReversalResponseMapper {

    static PaymentReversalResponse toResponse(ReversePaymentResult result) {
        return new PaymentReversalResponse(
                result.paymentId(),
                result.reversalId(),
                result.status(),
                result.reason(),
                result.correlationId(),
                result.reversedAt()
        );
    }
}
