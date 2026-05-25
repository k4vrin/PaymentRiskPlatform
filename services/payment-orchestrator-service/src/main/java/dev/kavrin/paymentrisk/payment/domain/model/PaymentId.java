package dev.kavrin.paymentrisk.payment.domain.model;

import java.util.UUID;

public record PaymentId(String value) {

    public PaymentId {
        value = RequiredText.require(value, "paymentId", 100);
    }

    public static PaymentId of(String value) {
        return new PaymentId(value);
    }

    public static PaymentId generate() {
        return new PaymentId("pay_" + UUID.randomUUID());
    }
}
