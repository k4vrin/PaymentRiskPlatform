package dev.kavrin.paymentrisk.payment.domain;

public record PaymentMethodToken(String value) {

    public PaymentMethodToken {
        value = RequiredText.require(value, "paymentMethodToken", 256);
    }

    public static PaymentMethodToken of(String value) {
        return new PaymentMethodToken(value);
    }

    public String masked() {
        if (value.length() <= 4) {
            return "****";
        }
        return "****" + value.substring(value.length() - 4);
    }
}
