package dev.kavrin.paymentrisk.payment.infrastructure.persistence;

import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import dev.kavrin.paymentrisk.payment.domain.model.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SensitivePaymentDataHasherTest {

    private static final Instant NOW = Instant.parse("2026-05-25T10:15:30Z");

    private final SensitivePaymentDataHasher hasher =
            SensitivePaymentDataHasher.withUtf8Key("test-hash-key");

    @Test
    void hashesPaymentMethodTokenDeterministically() {
        String hash = hasher.hashPaymentMethodToken(
                PaymentMethodToken.of("pmt_tok_sensitive_1234")
        );

        assertThat(hash)
                .isEqualTo("2358e3b4d35c95c7c95ac0181be83b8a6b1e93c12a0511c97cd6cc099ae136c2")
                .hasSize(64);
    }

    @Test
    void derivesPaymentMethodTokenLastFour() {
        assertThat(hasher.paymentMethodTokenLastFour(PaymentMethodToken.of("pmt_tok_sensitive_1234")))
                .isEqualTo("1234");
        assertThat(hasher.paymentMethodTokenLastFour(PaymentMethodToken.of("tok")))
                .isEqualTo("tok");
    }

    @Test
    void hashesDeviceFingerprintDeterministically() {
        String hash = hasher.hashDeviceFingerprint(
                DeviceFingerprint.of("dfp_sensitive_device_value")
        );

        assertThat(hash)
                .isEqualTo("a1ef4ab59fe54c172ef9b6b334c5330c0e0da34cd452a4ee3c85dcf712d899ba")
                .hasSize(64);
    }

    @Test
    void usesDifferentHashContextsForDifferentSensitiveFields() {
        String tokenHash = hasher.hashPaymentMethodToken(PaymentMethodToken.of("same-sensitive-value"));
        String fingerprintHash = hasher.hashDeviceFingerprint(DeviceFingerprint.of("same-sensitive-value"));

        assertThat(tokenHash).isNotEqualTo(fingerprintHash);
    }

    @Test
    void hashesAllSensitivePaymentDataForPersistence() {
        SensitivePaymentDataHasher.SensitivePaymentDataHashes hashes =
                hasher.hash(authorizedPayment());

        assertThat(hashes.paymentMethodTokenHash())
                .isEqualTo("2358e3b4d35c95c7c95ac0181be83b8a6b1e93c12a0511c97cd6cc099ae136c2");
        assertThat(hashes.paymentMethodTokenLastFour()).isEqualTo("1234");
        assertThat(hashes.deviceFingerprintHash())
                .isEqualTo("a1ef4ab59fe54c172ef9b6b334c5330c0e0da34cd452a4ee3c85dcf712d899ba");
    }

    @Test
    void rejectsMissingHashKey() {
        assertThatThrownBy(() -> SensitivePaymentDataHasher.withUtf8Key(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("hashKey must not be blank");
        assertThatThrownBy(() -> new SensitivePaymentDataHasher(new byte[0]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("hashKey must not be empty");
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
