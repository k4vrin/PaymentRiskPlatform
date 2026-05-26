package dev.kavrin.paymentrisk.payment.domain.model;

import dev.kavrin.paymentrisk.shared.id.PlatformIdGeneratorFactory;

public record PaymentId(String value) {

    private static final PlatformIdGeneratorFactory ID_GENERATOR = new PlatformIdGeneratorFactory();

    public PaymentId {
        value = RequiredText.require(value, "paymentId", 100);
    }

    public static PaymentId of(String value) {
        return new PaymentId(value);
    }

    public static PaymentId generate() {
        return new PaymentId(ID_GENERATOR.paymentId());
    }
}
