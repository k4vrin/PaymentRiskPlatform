package dev.kavrin.paymentrisk.payment.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import dev.kavrin.paymentrisk.payment.domain.model.*;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentAuthorizationEntity;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentEntity;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentRiskDecisionEntity;
import dev.kavrin.paymentrisk.shared.id.PlatformIdGeneratorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.core.ReactiveInsertOperation;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
class DurablePaymentStatePersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-05-25T10:15:30Z");

    private final R2dbcEntityTemplate entityTemplate = mock(R2dbcEntityTemplate.class);
    private final ReactiveInsertOperation.ReactiveInsert<Object> insertSpec =
            mock(ReactiveInsertOperation.ReactiveInsert.class);
    private final DurablePaymentStatePersistenceAdapter adapter =
            new DurablePaymentStatePersistenceAdapter(
                    entityTemplate,
                    new PaymentPersistenceMapper(
                            new ObjectMapper(),
                            new PlatformIdGeneratorFactory(),
                            Clock.fixed(NOW, ZoneOffset.UTC)
                    ),
                    SensitivePaymentDataHasher.withUtf8Key("test-hash-key")
            );

    @BeforeEach
    void configureRepositoryMocks() {
        reset(entityTemplate, insertSpec);
        when(entityTemplate.insert(any(Class.class)))
                .thenReturn(insertSpec);
        when(insertSpec.using(any()))
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

        verify(entityTemplate).insert(PaymentEntity.class);
        verify(entityTemplate).insert(PaymentAuthorizationEntity.class);
        verify(entityTemplate).insert(PaymentRiskDecisionEntity.class);
        verify(insertSpec).using(paymentEntityCaptor.capture());
        verify(insertSpec).using(authorizationEntityCaptor.capture());
        verify(insertSpec).using(riskDecisionEntityCaptor.capture());

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

        verify(entityTemplate).insert(PaymentEntity.class);
        verify(entityTemplate).insert(PaymentAuthorizationEntity.class);
        verify(entityTemplate, never()).insert(PaymentRiskDecisionEntity.class);
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
