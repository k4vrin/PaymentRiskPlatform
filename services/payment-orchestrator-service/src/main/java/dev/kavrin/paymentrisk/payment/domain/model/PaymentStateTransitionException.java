package dev.kavrin.paymentrisk.payment.domain.model;

public class PaymentStateTransitionException extends RuntimeException {

    public PaymentStateTransitionException(String message) {
        super(message);
    }
}