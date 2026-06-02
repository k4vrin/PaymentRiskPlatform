package dev.kavrin.paymentrisk.payment.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import dev.kavrin.paymentrisk.payment.domain.model.*;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentAuthorizationEntity;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentEntity;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentReversalEntity;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentRiskDecisionEntity;
import dev.kavrin.paymentrisk.shared.id.PlatformIdGeneratorFactory;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentPersistenceMapperTest {

    private static final Instant NOW = Instant.parse("2026-05-25T10:15:30Z");

    private final PaymentPersistenceMapper mapper = new PaymentPersistenceMapper(
            new ObjectMapper(),
            new PlatformIdGeneratorFactory(),
            Clock.fixed(NOW, ZoneOffset.UTC)
    );
    private final SensitivePaymentDataHasher sensitiveDataHasher =
            SensitivePaymentDataHasher.withUtf8Key("test-hash-key");

    @Test
    void mapsAuthorizedPaymentToPersistenceEntitiesWithoutRawSensitiveValues() {
        Payment payment = authorizedPayment();
        SensitivePaymentDataHasher.SensitivePaymentDataHashes hashes =
                sensitiveDataHasher.hash(payment);

        PaymentEntity paymentEntity = mapper.toPaymentEntity(payment, hashes);
        PaymentAuthorizationEntity authorizationEntity = mapper.toAuthorizationEntity(payment);
        PaymentRiskDecisionEntity riskDecisionEntity = mapper.toRiskDecisionEntity(payment);

        assertThat(paymentEntity.getPaymentId()).isEqualTo("pay_test");
        assertThat(paymentEntity.getPaymentMethodTokenHash())
                .isEqualTo("2358e3b4d35c95c7c95ac0181be83b8a6b1e93c12a0511c97cd6cc099ae136c2");
        assertThat(paymentEntity.getPaymentMethodTokenLast4()).isEqualTo("1234");
        assertThat(paymentEntity.getDeviceFingerprintHash())
                .isEqualTo("a1ef4ab59fe54c172ef9b6b334c5330c0e0da34cd452a4ee3c85dcf712d899ba");
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
        SensitivePaymentDataHasher.SensitivePaymentDataHashes hashes =
                sensitiveDataHasher.hash(payment);
        PaymentEntity paymentEntity = mapper.toPaymentEntity(payment, hashes);

        Payment restored = mapper.toDomain(
                paymentEntity,
                mapper.toAuthorizationEntity(payment),
                mapper.toRiskDecisionEntity(payment),
                null
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

    @Test
    void mapsReversedPaymentToReversalEntityWithReversalIdempotencyKey() {
        Payment payment = reversedPayment();

        PaymentReversalEntity reversalEntity = mapper.toReversalEntity(
                payment,
                IdempotencyKey.of("idem_reversal_01")
        );

        assertThat(reversalEntity.getPaymentReversalId()).isEqualTo("rev_test");
        assertThat(reversalEntity.getPaymentId()).isEqualTo("pay_test");
        assertThat(reversalEntity.getMerchantId()).isEqualTo("mer_test");
        assertThat(reversalEntity.getCustomerId()).isEqualTo("cus_test");
        assertThat(reversalEntity.getIdempotencyKey()).isEqualTo("idem_reversal_01");
        assertThat(reversalEntity.getReason()).isEqualTo("merchant_requested");
        assertThat(reversalEntity.getStatus()).isEqualTo("REVERSED");
        assertThat(reversalEntity.getRequestedAt()).isEqualTo(NOW.plusSeconds(30));
        assertThat(reversalEntity.getReversedAt()).isEqualTo(NOW.plusSeconds(30));
        assertThat(reversalEntity.getCreatedAt()).isEqualTo(NOW.plusSeconds(30));
        assertThat(reversalEntity.getUpdatedAt()).isEqualTo(NOW.plusSeconds(30));
    }

    @Test
    void mapsReversalEntityBackToDomainReversal() {
        PaymentReversal reversal = mapper.toDomainReversal(PaymentReversalEntity.builder()
                .paymentReversalId("rev_test")
                .paymentId("pay_test")
                .merchantId("mer_test")
                .customerId("cus_test")
                .idempotencyKey("idem_reversal_01")
                .reason("merchant_requested")
                .status("REVERSED")
                .requestedAt(NOW.plusSeconds(30))
                .reversedAt(NOW.plusSeconds(30))
                .createdAt(NOW.plusSeconds(30))
                .updatedAt(NOW.plusSeconds(30))
                .build());

        assertThat(reversal.reversalId()).isEqualTo(ReversalId.of("rev_test"));
        assertThat(reversal.paymentId()).isEqualTo(PaymentId.of("pay_test"));
        assertThat(reversal.reason()).isEqualTo(ReversalReason.of("merchant_requested"));
        assertThat(reversal.status()).isEqualTo(ReversalStatus.REVERSED);
        assertThat(reversal.requestedAt()).isEqualTo(NOW.plusSeconds(30));
        assertThat(reversal.reversedAt()).isEqualTo(NOW.plusSeconds(30));
    }

    @Test
    void restoresDomainPaymentWithReversalState() {
        Payment payment = reversedPayment();
        SensitivePaymentDataHasher.SensitivePaymentDataHashes hashes =
                sensitiveDataHasher.hash(payment);
        PaymentEntity paymentEntity = mapper.toPaymentEntity(payment, hashes);
        PaymentReversal reversal = mapper.toDomainReversal(
                mapper.toReversalEntity(payment, IdempotencyKey.of("idem_reversal_01"))
        );

        Payment restored = mapper.toDomain(
                paymentEntity,
                mapper.toAuthorizationEntity(payment),
                mapper.toRiskDecisionEntity(payment),
                reversal
        );

        assertThat(restored.getStatus()).isEqualTo(PaymentStatus.REVERSED);
        assertThat(restored.reversal()).contains(reversal);
        assertThat(restored.getAuthorization())
                .isInstanceOf(PaymentAuthorization.Authorized.class);
    }

    @Test
    void rejectsReversalEntityMappingWhenPaymentHasNoReversalState() {
        assertThatThrownBy(() ->
                mapper.toReversalEntity(
                        authorizedPayment(),
                        IdempotencyKey.of("idem_reversal_01")
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Payment has no reversal state");
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

    private static Payment reversedPayment() {
        Payment payment = authorizedPayment();
        payment.markReversed(
                ReversalId.of("rev_test"),
                ReversalReason.of("merchant_requested"),
                NOW.plusSeconds(30)
        );
        return payment;
    }
}
