package dev.kavrin.paymentrisk.payment.domain.model;

public record MerchantId(String value) {

    public MerchantId {
        value = RequiredText.require(value, "merchantId", 100);
    }

    public static MerchantId of(String value) {
        return new MerchantId(value);
    }
}
