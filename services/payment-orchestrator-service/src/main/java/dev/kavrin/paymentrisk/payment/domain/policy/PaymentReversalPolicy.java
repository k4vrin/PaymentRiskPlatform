package dev.kavrin.paymentrisk.payment.domain.policy;

import dev.kavrin.paymentrisk.payment.domain.model.Payment;
import dev.kavrin.paymentrisk.payment.domain.model.PaymentStateTransitionException;
import dev.kavrin.paymentrisk.payment.domain.model.PaymentStatus;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PaymentReversalPolicy {

    public static void assertReversible(Payment payment) {
        var status = payment.getStatus();

        if (status == PaymentStatus.AUTHORIZED) {
            return;
        }

        if (status == PaymentStatus.REVERSED) {
            throw new PaymentStateTransitionException(
                    "Payment is already reversed"
            );
        }

        throw new PaymentStateTransitionException(
                "Payment with status " + status + " cannot be reversed"
        );
    }
}