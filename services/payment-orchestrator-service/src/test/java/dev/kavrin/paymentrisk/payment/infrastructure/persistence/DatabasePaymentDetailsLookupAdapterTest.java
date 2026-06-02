package dev.kavrin.paymentrisk.payment.infrastructure.persistence;

import dev.kavrin.paymentrisk.TestPostgresConfiguration;
import dev.kavrin.paymentrisk.payment.domain.model.PaymentId;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentAuthorizationEntity;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentEntity;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentReversalEntity;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentRiskDecisionEntity;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.repository.PaymentAuthorizationEntityRepository;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.repository.PaymentEntityRepository;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.repository.PaymentReversalEntityRepository;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.repository.PaymentRiskDecisionEntityRepository;
import dev.kavrin.paymentrisk.shared.api.error.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration,"
                + "org.springframework.boot.data.redis.autoconfigure.DataRedisReactiveAutoConfiguration,"
                + "org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration,"
                + "org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration,"
                + "org.springframework.boot.security.autoconfigure.web.reactive.ReactiveWebSecurityAutoConfiguration,"
                + "org.springframework.boot.security.autoconfigure.actuate.web.reactive.ReactiveManagementWebSecurityAutoConfiguration"
})
@ActiveProfiles("test")
@Import(TestPostgresConfiguration.class)
class DatabasePaymentDetailsLookupAdapterTest {

    private static final Instant NOW = Instant.parse("2026-05-25T10:15:30Z");

    @Autowired
    private DatabasePaymentDetailsLookupAdapter adapter;

    @Autowired
    private PaymentEntityRepository paymentRepository;

    @Autowired
    private PaymentAuthorizationEntityRepository authorizationRepository;

    @Autowired
    private PaymentRiskDecisionEntityRepository riskDecisionRepository;

    @Autowired
    private PaymentReversalEntityRepository reversalRepository;

    @Autowired
    private R2dbcEntityTemplate entityTemplate;

    @BeforeEach
    void deleteExistingRecords() {
        reversalRepository.deleteAll().block();
        riskDecisionRepository.deleteAll().block();
        authorizationRepository.deleteAll().block();
        paymentRepository.deleteAll().block();
    }

    @Test
    void readsFullPaymentDetailsFromPersistenceTables() {
        insertPayment("pay_full", "REVERSED");
        insertAuthorization("pay_full");
        insertRiskDecision("pay_full");
        insertReversal("pay_full");

        StepVerifier.create(adapter.findByPaymentId(PaymentId.of("pay_full")))
                .assertNext(result -> {
                    assertThat(result.paymentId()).isEqualTo("pay_full");
                    assertThat(result.merchantId()).isEqualTo("mer_test");
                    assertThat(result.customerId()).isEqualTo("cus_test");
                    assertThat(result.amountMinor()).isEqualTo(1299);
                    assertThat(result.currency()).isEqualTo("USD");
                    assertThat(result.status()).isEqualTo("REVERSED");
                    assertThat(result.externalReference()).isEqualTo("order_2026_000123");
                    assertThat(result.createdAt()).isEqualTo(NOW);
                    assertThat(result.updatedAt()).isEqualTo(NOW.plusSeconds(30));

                    assertThat(result.authorization().status()).isEqualTo("AUTHORIZED");
                    assertThat(result.authorization().authorizationCode()).isEqualTo("AUTH-ABCDEFG123");
                    assertThat(result.authorization().requestedAt()).isEqualTo(NOW);
                    assertThat(result.authorization().riskPendingAt()).isEqualTo(NOW.plusSeconds(5));
                    assertThat(result.authorization().authorizedAt()).isEqualTo(NOW.plusSeconds(10));

                    assertThat(result.risk().decision()).isEqualTo("APPROVED");
                    assertThat(result.risk().score()).isEqualTo(7);
                    assertThat(result.risk().reasonCodes()).containsExactly("LOW_RISK", "CVV_MATCH");
                    assertThat(result.risk().ruleVersion()).isEqualTo("risk-rules-v1");
                    assertThat(result.risk().decidedAt()).isEqualTo(NOW.plusSeconds(8));

                    assertThat(result.reversal().reversalId()).isEqualTo("rev_full");
                    assertThat(result.reversal().status()).isEqualTo("REVERSED");
                    assertThat(result.reversal().reason()).isEqualTo("merchant_requested");
                    assertThat(result.reversal().requestedAt()).isEqualTo(NOW.plusSeconds(20));
                    assertThat(result.reversal().reversedAt()).isEqualTo(NOW.plusSeconds(30));
                })
                .verifyComplete();
    }

    @Test
    void readsPartialPaymentDetailsWhenOptionalRowsAreMissing() {
        insertPayment("pay_partial", "RECEIVED");
        insertRequestedAuthorization("pay_partial");

        StepVerifier.create(adapter.findByPaymentId(PaymentId.of("pay_partial")))
                .assertNext(result -> {
                    assertThat(result.paymentId()).isEqualTo("pay_partial");
                    assertThat(result.status()).isEqualTo("RECEIVED");
                    assertThat(result.authorization().status()).isEqualTo("REQUESTED");
                    assertThat(result.authorization().authorizationCode()).isNull();
                    assertThat(result.risk()).isNull();
                    assertThat(result.reversal()).isNull();
                })
                .verifyComplete();
    }

    @Test
    void returnsNotFoundWhenPaymentDoesNotExist() {
        StepVerifier.create(adapter.findByPaymentId(PaymentId.of("pay_missing")))
                .expectErrorSatisfies(error -> {
                    assertThat(error)
                            .isInstanceOf(ResourceNotFoundException.class)
                            .hasMessage("Payment not found: pay_missing");
                })
                .verify();
    }

    private void insertPayment(String paymentId, String status) {
        entityTemplate.insert(PaymentEntity.class)
                .using(PaymentEntity.builder()
                        .paymentId(paymentId)
                        .merchantId("mer_test")
                        .customerId("cus_test")
                        .amountMinor(1299)
                        .currency("USD")
                        .paymentMethodTokenHash("token_hash_should_not_be_exposed")
                        .paymentMethodTokenLast4("1234")
                        .deviceFingerprintHash("device_hash_should_not_be_exposed")
                        .externalReference("order_2026_000123")
                        .idempotencyKey("idem_" + paymentId)
                        .status(status)
                        .createdAt(NOW)
                        .updatedAt(status.equals("REVERSED") ? NOW.plusSeconds(30) : NOW)
                        .build())
                .block();
    }

    private void insertAuthorization(String paymentId) {
        entityTemplate.insert(PaymentAuthorizationEntity.class)
                .using(PaymentAuthorizationEntity.builder()
                        .paymentAuthorizationId("pauth_" + paymentId)
                        .paymentId(paymentId)
                        .status("AUTHORIZED")
                        .authorizationCode("AUTH-ABCDEFG123")
                        .requestedAt(NOW)
                        .riskPendingAt(NOW.plusSeconds(5))
                        .authorizedAt(NOW.plusSeconds(10))
                        .createdAt(NOW)
                        .updatedAt(NOW.plusSeconds(10))
                        .build())
                .block();
    }

    private void insertRequestedAuthorization(String paymentId) {
        entityTemplate.insert(PaymentAuthorizationEntity.class)
                .using(PaymentAuthorizationEntity.builder()
                        .paymentAuthorizationId("pauth_" + paymentId)
                        .paymentId(paymentId)
                        .status("REQUESTED")
                        .requestedAt(NOW)
                        .createdAt(NOW)
                        .updatedAt(NOW)
                        .build())
                .block();
    }

    private void insertRiskDecision(String paymentId) {
        entityTemplate.insert(PaymentRiskDecisionEntity.class)
                .using(PaymentRiskDecisionEntity.builder()
                        .paymentRiskDecisionId("prd_" + paymentId)
                        .paymentId(paymentId)
                        .decision("APPROVED")
                        .score(7)
                        .reasonCodesJson("[\"LOW_RISK\",\"CVV_MATCH\"]")
                        .ruleVersion("risk-rules-v1")
                        .decidedAt(NOW.plusSeconds(8))
                        .createdAt(NOW.plusSeconds(8))
                        .build())
                .block();
    }

    private void insertReversal(String paymentId) {
        entityTemplate.insert(PaymentReversalEntity.class)
                .using(PaymentReversalEntity.builder()
                        .paymentReversalId("rev_full")
                        .paymentId(paymentId)
                        .merchantId("mer_test")
                        .customerId("cus_test")
                        .idempotencyKey("idem_reversal_01")
                        .reason("merchant_requested")
                        .status("REVERSED")
                        .requestedAt(NOW.plusSeconds(20))
                        .reversedAt(NOW.plusSeconds(30))
                        .createdAt(NOW.plusSeconds(20))
                        .updatedAt(NOW.plusSeconds(30))
                        .build())
                .block();
    }
}
