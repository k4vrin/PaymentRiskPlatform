package dev.kavrin.paymentrisk.payment.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import dev.kavrin.paymentrisk.payment.domain.model.*;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentAuthorizationRow;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentRiskDecisionRow;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentRow;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentPersistenceMapperTest {

    private static final Instant NOW = Instant.parse("2026-05-25T10:15:30Z");

    private final PaymentPersistenceMapper mapper = new PaymentPersistenceMapper(new ObjectMapper());

    @Test
    void mapsAuthorizedPaymentToPersistenceRowsWithoutRawSensitiveValues() {
        Payment payment = authorizedPayment();

        PaymentRow paymentRow = mapper.toPaymentRow(
                payment,
                "hmac-token-hash",
                "1234",
                "hmac-device-hash"
        );
        PaymentAuthorizationRow authorizationRow = mapper.toAuthorizationRow(payment);
        PaymentRiskDecisionRow riskDecisionRow = mapper.toRiskDecisionRow(payment);

        assertThat(paymentRow.getPaymentId()).isEqualTo("pay_test");
        assertThat(paymentRow.getPaymentMethodTokenHash()).isEqualTo("hmac-token-hash");
        assertThat(paymentRow.getPaymentMethodTokenLast4()).isEqualTo("1234");
        assertThat(paymentRow.getDeviceFingerprintHash()).isEqualTo("hmac-device-hash");
        assertThat(paymentRow.getStatus()).isEqualTo("AUTHORIZED");

        assertThat(authorizationRow.getPaymentAuthorizationId()).startsWith("pauth_");
        assertThat(authorizationRow.getPaymentId()).isEqualTo("pay_test");
        assertThat(authorizationRow.getStatus()).isEqualTo("AUTHORIZED");
        assertThat(authorizationRow.getAuthorizationCode()).isEqualTo("AUTH-ABCDEFG123");
        assertThat(authorizationRow.getAuthorizedAt()).isEqualTo(NOW);

        assertThat(riskDecisionRow.getPaymentRiskDecisionId()).startsWith("prd_");
        assertThat(riskDecisionRow.getPaymentId()).isEqualTo("pay_test");
        assertThat(riskDecisionRow.getDecision()).isEqualTo("APPROVED");
        assertThat(riskDecisionRow.getReasonCodesJson()).isEqualTo("[\"LOW_RISK\"]");
    }

    @Test
    void restoresDomainPaymentFromPersistenceRowsWithRedactedSensitiveValues() {
        Payment payment = authorizedPayment();
        PaymentRow paymentRow = mapper.toPaymentRow(
                payment,
                "hmac-token-hash",
                "1234",
                "hmac-device-hash"
        );

        Payment restored = mapper.toDomain(
                paymentRow,
                mapper.toAuthorizationRow(payment),
                mapper.toRiskDecisionRow(payment)
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
