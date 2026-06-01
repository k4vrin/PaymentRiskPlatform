package dev.kavrin.paymentrisk.payment.infrastructure.persistence.repository;

import dev.kavrin.paymentrisk.TestPostgresConfiguration;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentEntity;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentReversalEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
class PaymentReversalEntityRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-05-25T10:15:30Z");

    @Autowired
    private PaymentEntityRepository paymentRepository;

    @Autowired
    private PaymentReversalEntityRepository reversalRepository;

    @Autowired
    private R2dbcEntityTemplate entityTemplate;

    @BeforeEach
    void deleteExistingRecords() {
        reversalRepository.deleteAll().block();
        paymentRepository.deleteAll().block();
    }

    @Test
    void insertsAndReadsPaymentReversalByPaymentAndIdempotencyKey() {
        insertPayment("pay_test");

        PaymentReversalEntity saved = insert(reversal(
                "rev_01",
                "pay_test",
                "idem_rev_01"
        ));

        assertThat(saved.getPaymentReversalId()).isEqualTo("rev_01");

        PaymentReversalEntity byPayment = reversalRepository.findByPaymentId("pay_test").block();
        assertThat(byPayment).isNotNull();
        assertThat(byPayment.getPaymentReversalId()).isEqualTo("rev_01");
        assertThat(byPayment.getMerchantId()).isEqualTo("mer_test");
        assertThat(byPayment.getCustomerId()).isEqualTo("cus_test");
        assertThat(byPayment.getIdempotencyKey()).isEqualTo("idem_rev_01");
        assertThat(byPayment.getReason()).isEqualTo("merchant_request");
        assertThat(byPayment.getStatus()).isEqualTo("REVERSED");
        assertThat(byPayment.getRequestedAt()).isEqualTo(NOW);
        assertThat(byPayment.getReversedAt()).isEqualTo(NOW.plusSeconds(30));
        assertThat(byPayment.getCreatedAt()).isEqualTo(NOW);
        assertThat(byPayment.getUpdatedAt()).isEqualTo(NOW.plusSeconds(30));

        PaymentReversalEntity byIdempotencyKey = reversalRepository
                .findByIdempotencyKey("idem_rev_01")
                .block();
        assertThat(byIdempotencyKey).isNotNull();
        assertThat(byIdempotencyKey.getPaymentId()).isEqualTo("pay_test");
    }

    @Test
    void rejectsSecondReversalForSamePayment() {
        insertPayment("pay_test");
        insert(reversal("rev_01", "pay_test", "idem_rev_01"));

        assertThatThrownBy(() -> insert(reversal("rev_02", "pay_test", "idem_rev_02")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsDuplicateReversalIdempotencyKey() {
        insertPayment("pay_test_1");
        insertPayment("pay_test_2");
        insert(reversal("rev_01", "pay_test_1", "idem_rev_01"));

        assertThatThrownBy(() -> insert(reversal("rev_02", "pay_test_2", "idem_rev_01")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void insertPayment(String paymentId) {
        entityTemplate.insert(PaymentEntity.class)
                .using(PaymentEntity.builder()
                        .paymentId(paymentId)
                        .merchantId("mer_test")
                        .customerId("cus_test")
                        .amountMinor(1299)
                        .currency("USD")
                        .paymentMethodTokenHash("token_hash")
                        .paymentMethodTokenLast4("1234")
                        .deviceFingerprintHash("device_hash")
                        .externalReference("order_2026_000123")
                        .idempotencyKey("idem_" + paymentId)
                        .status("AUTHORIZED")
                        .createdAt(NOW)
                        .updatedAt(NOW)
                        .build())
                .block();
    }

    private PaymentReversalEntity insert(PaymentReversalEntity reversal) {
        return entityTemplate.insert(PaymentReversalEntity.class)
                .using(reversal)
                .block();
    }

    private static PaymentReversalEntity reversal(
            String reversalId,
            String paymentId,
            String idempotencyKey
    ) {
        return PaymentReversalEntity.builder()
                .paymentReversalId(reversalId)
                .paymentId(paymentId)
                .merchantId("mer_test")
                .customerId("cus_test")
                .idempotencyKey(idempotencyKey)
                .reason("merchant_request")
                .status("REVERSED")
                .requestedAt(NOW)
                .reversedAt(NOW.plusSeconds(30))
                .createdAt(NOW)
                .updatedAt(NOW.plusSeconds(30))
                .build();
    }
}
