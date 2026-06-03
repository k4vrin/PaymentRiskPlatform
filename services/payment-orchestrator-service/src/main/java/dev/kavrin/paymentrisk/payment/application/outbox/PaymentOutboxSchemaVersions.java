package dev.kavrin.paymentrisk.payment.application.outbox;

public final class PaymentOutboxSchemaVersions {

    public static final String PAYMENT_AUTHORIZATION_REQUESTED_V1 = "v1";
    public static final String PAYMENT_AUTHORIZED_V1 = "v1";
    public static final String PAYMENT_DECLINED_V1 = "v1";
    public static final String PAYMENT_REVERSED_V1 = "v1";

    private PaymentOutboxSchemaVersions() {
    }
}
