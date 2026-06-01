package dev.kavrin.paymentrisk.payment.application.outbox;

import java.time.Instant;
import java.util.List;

public record PaymentDeclinedPayload(
        String schemaVersion,
        String paymentId,
        String merchantId,
        String customerId,
        long amountMinor,
        String currency,
        int riskScore,
        List<String> reasonCodes,
        String ruleVersion,
        Instant declinedAt
) {
    public PaymentDeclinedPayload {
        reasonCodes = List.copyOf(reasonCodes);
    }

    public static PaymentDeclinedPayload v1(
            String paymentId,
            String merchantId,
            String customerId,
            long amountMinor,
            String currency,
            int riskScore,
            List<String> reasonCodes,
            String ruleVersion,
            Instant declinedAt
    ) {
        return new PaymentDeclinedPayload(
                PaymentOutboxSchemaVersions.PAYMENT_DECLINED_V1,
                paymentId,
                merchantId,
                customerId,
                amountMinor,
                currency,
                riskScore,
                reasonCodes,
                ruleVersion,
                declinedAt
        );
    }
}
