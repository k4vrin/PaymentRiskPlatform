package dev.kavrin.paymentrisk.payment.domain.model;

import dev.kavrin.paymentrisk.shared.id.PlatformIdGeneratorFactory;

public record PaymentId(String value) {

    private static final PlatformIdGeneratorFactory ID_GENERATOR = new PlatformIdGeneratorFactory();
    private static final String PAYMENT_ID_PATTERN = "pay_[A-Za-z0-9_-]+";

    public PaymentId {
        value = RequiredText.require(value, "paymentId", 100);
        if (!value.matches(PAYMENT_ID_PATTERN)) {
            throw new IllegalArgumentException(
                    "paymentId must start with pay_ and contain only letters, numbers, underscore, and hyphen."
            );
        }
    }

    public static PaymentId of(String value) {
        return new PaymentId(value);
    }

    public static PaymentId generate() {
        return new PaymentId(ID_GENERATOR.paymentId());
    }
}
