package dev.kavrin.paymentrisk.shared.id;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PlatformIdGeneratorFactory {

    public String paymentId() {
        return prefixedUuid("pay");
    }

    public String paymentAuthorizationId() {
        return prefixedCompactUuid("pauth");
    }

    public String paymentRiskDecisionId() {
        return prefixedCompactUuid("prd");
    }

    public String idempotencyRecordId() {
        return prefixedCompactUuid("idem_rec");
    }

    public String outboxEventId() {
        return prefixedCompactUuid("evt");
    }

    public String correlationId() {
        return UUID.randomUUID().toString();
    }

    private static String prefixedUuid(String prefix) {
        return prefix + "_" + UUID.randomUUID();
    }

    private static String prefixedCompactUuid(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}
