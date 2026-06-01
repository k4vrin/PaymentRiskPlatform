package dev.kavrin.paymentrisk.payment.application.outbox;

import java.time.Instant;
import java.util.List;

public record PaymentAuthorizedPayload(
        String schemaVersion,
        String paymentId,
        String merchantId,
        String customerId,
        long amountMinor,
        String currency,
        String authorizationCode,
        int riskScore,
        List<String> reasonCodes,
        String ruleVersion,
        Instant authorizedAt
) {
    public PaymentAuthorizedPayload {
        // makes the list immutable, so event payloads cannot be accidentally mutated after creation.
        reasonCodes = List.copyOf(reasonCodes);
    }

    public static PaymentAuthorizedPayload v1(
            String paymentId,
            String merchantId,
            String customerId,
            long amountMinor,
            String currency,
            String authorizationCode,
            int riskScore,
            List<String> reasonCodes,
            String ruleVersion,
            Instant authorizedAt
    ) {
        return new PaymentAuthorizedPayload(
                PaymentOutboxSchemaVersions.PAYMENT_AUTHORIZED_V1,
                paymentId,
                merchantId,
                customerId,
                amountMinor,
                currency,
                authorizationCode,
                riskScore,
                reasonCodes,
                ruleVersion,
                authorizedAt
        );
    }
}
