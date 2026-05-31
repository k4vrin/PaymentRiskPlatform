package dev.kavrin.paymentrisk.payment.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import dev.kavrin.paymentrisk.payment.domain.model.*;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentAuthorizationEntity;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentEntity;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentRiskDecisionEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentPersistenceMapperTest {

    private static final Instant NOW = Instant.parse("2026-05-25T10:15:30Z");

    private final PaymentPersistenceMapper mapper = new PaymentPersistenceMapper(new ObjectMapper());

    @Test
    void mapsAuthorizedPaymentToPersistenceEntitiesWithoutRawSensitiveValues() {
        Payment payment = authorizedPayment();

        PaymentEntity paymentEntity = mapper.toPaymentEntity(
                payment,
                "hmac-token-hash",
                "1234",
                "hmac-device-hash"
        );
        PaymentAuthorizationEntity authorizationEntity = mapper.toAuthorizationEntity(payment);
        PaymentRiskDecisionEntity riskDecisionEntity = mapper.toRiskDecisionEntity(payment);

        assertThat(paymentEntity.getPaymentId()).isEqualTo("pay_test");
        assertThat(paymentEntity.getPaymentMethodTokenHash()).isEqualTo("hmac-token-hash");
        assertThat(paymentEntity.getPaymentMethodTokenLast4()).isEqualTo("1234");
        assertThat(paymentEntity.getDeviceFingerprintHash()).isEqualTo("hmac-device-hash");
        assertThat(paymentEntity.getStatus()).isEqualTo("AUTHORIZED");

        assertThat(authorizationEntity.getPaymentAuthorizationId()).startsWith("pauth_");
        assertThat(authorizationEntity.getPaymentId()).isEqualTo("pay_test");
        assertThat(authorizationEntity.getStatus()).isEqualTo("AUTHORIZED");
        assertThat(authorizationEntity.getAuthorizationCode()).isEqualTo("AUTH-ABCDEFG123");
        assertThat(authorizationEntity.getAuthorizedAt()).isEqualTo(NOW);

        assertThat(riskDecisionEntity.getPaymentRiskDecisionId()).startsWith("prd_");
        assertThat(riskDecisionEntity.getPaymentId()).isEqualTo("pay_test");
        assertThat(riskDecisionEntity.getDecision()).isEqualTo("APPROVED");
        assertThat(riskDecisionEntity.getReasonCodesJson()).isEqualTo("[\"LOW_RISK\"]");
    }

    @Test
    void restoresDomainPaymentFromPersistenceEntitiesWithRedactedSensitiveValues() {
        Payment payment = authorizedPayment();
        PaymentEntity paymentEntity = mapper.toPaymentEntity(
                payment,
                "hmac-token-hash",
                "1234",
                "hmac-device-hash"
        );

        Payment restored = mapper.toDomain(
                paymentEntity,
                mapper.toAuthorizationEntity(payment),
                mapper.toRiskDecisionEntity(payment)
        );

        assertThat(restored.getId()).isEqualTo(PaymentId.of("pay_test"));
        assertThat(restored.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(restored.getPaymentMethodToken().value())
                .isEqualTo("restored-masked-payment-method-token");
        assertThat(restored.getDeviceFingerprint().value())
                .isEqualTo("restored-masked-device-fingerprint");
        assertThat(restored.getRiskDecision().reasonCodes()).containsExactly("LOW_RISK");
        assertThat(restored.getAuthorization())
                .isInstanceOf(PaymentAuthorization.Authorized.class);
    }

    private static Payment authorizedPayment() {
        Payment payment = Payment.newAuthorizationAttempt(
                PaymentId.of("pay_test"),
                MerchantId.of("mer_test"),
                CustomerId.of("cus_test"),
                Money.of(1299, "USD"),
                PaymentMethodToken.of("pmt_tok_sensitive_1234"),
                DeviceFingerprint.of("dfp_sensitive_device_value"),
                ExternalReference.of("order_2026_000123"),
                IdempotencyKey.of("idem_01HX7QK9JP7E5W5NRZ6T5Q3R1A"),
                NOW
        );

        payment.markRiskPending(NOW);
        payment.markAuthorized(
                new PaymentRiskDecision(
                        RiskDecision.APPROVED,
                        7,
                        List.of("LOW_RISK"),
                        "risk-rules-v1",
                        NOW
                ),
                AuthorizationCode.of("AUTH-ABCDEFG123"),
                NOW
        );
        return payment;
    }
}
