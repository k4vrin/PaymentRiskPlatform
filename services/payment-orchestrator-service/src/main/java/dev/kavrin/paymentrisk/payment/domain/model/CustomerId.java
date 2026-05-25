package dev.kavrin.paymentrisk.payment.domain.model;

public record CustomerId(String value) {

    public CustomerId {
        value = RequiredText.require(value, "customerId", 100);
    }

    public static CustomerId of(String value) {
        return new CustomerId(value);
    }
}
