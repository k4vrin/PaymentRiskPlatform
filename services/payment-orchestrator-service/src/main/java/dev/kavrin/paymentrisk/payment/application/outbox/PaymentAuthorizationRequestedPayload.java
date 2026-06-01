package dev.kavrin.paymentrisk.payment.application.outbox;

import java.time.Instant;

public record PaymentAuthorizationRequestedPayload(
        String schemaVersion,
        String paymentId,
        String merchantId,
        String customerId,
        long amountMinor,
        String currency,
        String externalReference,
        Instant requestedAt
) {
    public static PaymentAuthorizationRequestedPayload v1(
            String paymentId,
            String merchantId,
            String customerId,
            long amountMinor,
            String currency,
            String externalReference,
            Instant requestedAt
    ) {
        return new PaymentAuthorizationRequestedPayload(
                PaymentOutboxSchemaVersions.PAYMENT_AUTHORIZATION_REQUESTED_V1,
                paymentId,
                merchantId,
                customerId,
                amountMinor,
                currency,
                externalReference,
                requestedAt
        );
    }
}
