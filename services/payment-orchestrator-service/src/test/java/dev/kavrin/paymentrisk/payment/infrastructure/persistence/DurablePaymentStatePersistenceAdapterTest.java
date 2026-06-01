package dev.kavrin.paymentrisk.payment.infrastructure.persistence;

import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import dev.kavrin.paymentrisk.payment.domain.model.*;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentAuthorizationEntity;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentEntity;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentRiskDecisionEntity;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.repository.PaymentAuthorizationEntityRepository;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.repository.PaymentEntityRepository;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.repository.PaymentRiskDecisionEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
class DurablePaymentStatePersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-05-25T10:15:30Z");

    private final PaymentEntityRepository paymentRepository =
            mock(PaymentEntityRepository.class);
    private final PaymentAuthorizationEntityRepository authorizationRepository =
            mock(PaymentAuthorizationEntityRepository.class);
    private final PaymentRiskDecisionEntityRepository riskDecisionRepository =
            mock(PaymentRiskDecisionEntityRepository.class);
    private final DurablePaymentStatePersistenceAdapter adapter =
            new DurablePaymentStatePersistenceAdapter(
                    paymentRepository,
                    authorizationRepository,
                    riskDecisionRepository,
                    new PaymentPersistenceMapper(),
                    SensitivePaymentDataHasher.withUtf8Key("test-hash-key")
            );

    @BeforeEach
    void configureRepositoryMocks() {
        reset(paymentRepository, authorizationRepository, riskDecisionRepository);
        when(paymentRepository.save(any(PaymentEntity.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(authorizationRepository.save(any(PaymentAuthorizationEntity.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(riskDecisionRepository.save(any(PaymentRiskDecisionEntity.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    }

    @Test
    void savePersistsPaymentAuthorizationAndRiskDecisionEntities() {
        Payment payment = authorizedPayment();

        StepVerifier.create(adapter.save(payment))
                .expectNext(payment)
                .verifyComplete();

        ArgumentCaptor<PaymentEntity> paymentEntityCaptor =
                ArgumentCaptor.forClass(PaymentEntity.class);
        ArgumentCaptor<PaymentAuthorizationEntity> authorizationEntityCaptor =
                ArgumentCaptor.forClass(PaymentAuthorizationEntity.class);
        ArgumentCaptor<PaymentRiskDecisionEntity> riskDecisionEntityCaptor =
                ArgumentCaptor.forClass(PaymentRiskDecisionEntity.class);

        verify(paymentRepository).save(paymentEntityCaptor.capture());
        verify(authorizationRepository).save(authorizationEntityCaptor.capture());
        verify(riskDecisionRepository).save(riskDecisionEntityCaptor.capture());

        PaymentEntity paymentEntity = paymentEntityCaptor.getValue();
        assertThat(paymentEntity.getPaymentId()).isEqualTo("pay_test");
        assertThat(paymentEntity.getStatus()).isEqualTo("AUTHORIZED");
        assertThat(paymentEntity.getPaymentMethodTokenHash())
                .isNotEqualTo("pmt_tok_sensitive_1234");
        assertThat(paymentEntity.getPaymentMethodTokenLast4()).isEqualTo("1234");
        assertThat(paymentEntity.getDeviceFingerprintHash())
                .isNotEqualTo("dfp_sensitive_device_value");

        PaymentAuthorizationEntity authorizationEntity =
                authorizationEntityCaptor.getValue();
        assertThat(authorizationEntity.getPaymentId()).isEqualTo("pay_test");
        assertThat(authorizationEntity.getStatus()).isEqualTo("AUTHORIZED");
        assertThat(authorizationEntity.getAuthorizationCode()).isEqualTo("AUTH-ABCDEFG123");
        assertThat(authorizationEntity.getAuthorizedAt()).isEqualTo(NOW);

        PaymentRiskDecisionEntity riskDecisionEntity =
                riskDecisionEntityCaptor.getValue();
        assertThat(riskDecisionEntity.getPaymentId()).isEqualTo("pay_test");
        assertThat(riskDecisionEntity.getDecision()).isEqualTo("APPROVED");
        assertThat(riskDecisionEntity.getScore()).isEqualTo(7);
        assertThat(riskDecisionEntity.getReasonCodesJson()).isEqualTo("[\"LOW_RISK\"]");
    }

    @Test
    void saveSkipsRiskDecisionEntityWhenPaymentHasNoRiskDecision() {
        Payment payment = requestedPayment();

        StepVerifier.create(adapter.save(payment))
                .expectNext(payment)
                .verifyComplete();

        verify(paymentRepository).save(any(PaymentEntity.class));
        verify(authorizationRepository).save(any(PaymentAuthorizationEntity.class));
        verifyNoInteractions(riskDecisionRepository);
    }

    private static Payment requestedPayment() {
        return Payment.newAuthorizationAttempt(
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
    }

    private static Payment authorizedPayment() {
        Payment payment = requestedPayment();

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
