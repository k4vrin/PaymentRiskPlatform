package dev.kavrin.paymentrisk.ops.infrastructure;

import dev.kavrin.paymentrisk.TestPostgresConfiguration;
import dev.kavrin.paymentrisk.ops.application.OpsPaymentSearchRequest;
import dev.kavrin.paymentrisk.payment.domain.model.CustomerId;
import dev.kavrin.paymentrisk.payment.domain.model.MerchantId;
import dev.kavrin.paymentrisk.payment.domain.model.PaymentId;
import dev.kavrin.paymentrisk.payment.domain.model.PaymentStatus;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentAuthorizationEntity;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentEntity;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentReversalEntity;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentRiskDecisionEntity;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.repository.PaymentAuthorizationEntityRepository;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.repository.PaymentEntityRepository;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.repository.PaymentReversalEntityRepository;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.repository.PaymentRiskDecisionEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Optional;

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
class DatabaseOpsPaymentSearchAdapterTest {

    private static final Instant BASE_TIME = Instant.parse("2026-06-04T10:00:00Z");

    @Autowired
    private DatabaseOpsPaymentSearchAdapter adapter;

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
    void searchesPaymentsWithCombinedFiltersAndJoinedSummaries() {
        insertPayment("pay_ops_001", "mer_ops_a", "cus_ops_a", "AUTHORIZED", BASE_TIME.plusSeconds(30));
        insertAuthorization("pay_ops_001", "AUTHORIZED");
        insertRiskDecision("pay_ops_001", "APPROVED", 12);
        insertReversal("pay_ops_001", "REVERSED");

        insertPayment("pay_ops_002", "mer_ops_a", "cus_ops_b", "DECLINED", BASE_TIME.plusSeconds(20));
        insertPayment("pay_ops_003", "mer_ops_b", "cus_ops_a", "AUTHORIZED", BASE_TIME.plusSeconds(10));

        var request = OpsPaymentSearchRequest.firstPage(
                Optional.of(PaymentStatus.AUTHORIZED),
                Optional.of(MerchantId.of("mer_ops_a")),
                Optional.of(CustomerId.of("cus_ops_a")),
                Optional.empty(),
                Optional.of(BASE_TIME),
                Optional.of(BASE_TIME.plusSeconds(60)),
                25
        );

        var result = adapter.search(request).block();

        assertThat(result).isNotNull();
        assertThat(result.items()).hasSize(1);
        assertThat(result.nextPageToken()).isEmpty();

        var item = result.items().getFirst();
        assertThat(item.paymentId()).isEqualTo("pay_ops_001");
        assertThat(item.merchantId()).isEqualTo("mer_ops_a");
        assertThat(item.customerId()).isEqualTo("cus_ops_a");
        assertThat(item.amountMinor()).isEqualTo(1299);
        assertThat(item.currency()).isEqualTo("USD");
        assertThat(item.status()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(item.externalReference()).contains("order_pay_ops_001");
        assertThat(item.createdAt()).isEqualTo(BASE_TIME.plusSeconds(30));

        assertThat(item.authorization()).isPresent();
        assertThat(item.authorization().orElseThrow().authorizationStatus()).isEqualTo("AUTHORIZED");
        assertThat(item.authorization().orElseThrow().authorizationCode()).contains("AUTH-pay_ops_001");

        assertThat(item.risk()).isPresent();
        assertThat(item.risk().orElseThrow().decision()).isEqualTo("APPROVED");
        assertThat(item.risk().orElseThrow().score()).isEqualTo(12);

        assertThat(item.reversal()).isPresent();
        assertThat(item.reversal().orElseThrow().reversalId()).isEqualTo("rev_pay_ops_001");
        assertThat(item.reversal().orElseThrow().status()).isEqualTo("REVERSED");
    }

    @Test
    void filtersByPaymentId() {
        insertPayment("pay_ops_004", "mer_ops_a", "cus_ops_a", "AUTHORIZED", BASE_TIME.plusSeconds(10));
        insertPayment("pay_ops_005", "mer_ops_a", "cus_ops_a", "AUTHORIZED", BASE_TIME.plusSeconds(20));

        var request = OpsPaymentSearchRequest.firstPage(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(PaymentId.of("pay_ops_004")),
                Optional.empty(),
                Optional.empty(),
                25
        );

        var result = adapter.search(request).block();

        assertThat(result).isNotNull();
        assertThat(result.items())
                .extracting("paymentId")
                .containsExactly("pay_ops_004");
    }

    @Test
    void returnsEmptyResultWhenFiltersDoNotMatch() {
        insertPayment("pay_ops_006", "mer_ops_a", "cus_ops_a", "AUTHORIZED", BASE_TIME);

        var request = OpsPaymentSearchRequest.firstPage(
                Optional.of(PaymentStatus.DECLINED),
                Optional.of(MerchantId.of("mer_ops_missing")),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                25
        );

        var result = adapter.search(request).block();

        assertThat(result).isNotNull();
        assertThat(result.items()).isEmpty();
        assertThat(result.nextPageToken()).isEmpty();
    }

    @Test
    void pagesResultsWithCompositeCursor() {
        insertPayment("pay_ops_old", "mer_ops_a", "cus_ops_a", "AUTHORIZED", BASE_TIME.plusSeconds(10));
        insertPayment("pay_ops_mid", "mer_ops_a", "cus_ops_a", "AUTHORIZED", BASE_TIME.plusSeconds(20));
        insertPayment("pay_ops_new", "mer_ops_a", "cus_ops_a", "AUTHORIZED", BASE_TIME.plusSeconds(30));

        var firstPage = OpsPaymentSearchRequest.firstPage(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                2
        );

        var result = adapter.search(firstPage).block();

        assertThat(result).isNotNull();
        assertThat(result.items())
                .extracting("paymentId")
                .containsExactly("pay_ops_new", "pay_ops_mid");
        assertThat(result.nextPageToken()).isPresent();

        var secondPage = new OpsPaymentSearchRequest(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                2,
                result.nextPageToken()
        );
        var nextResult = adapter.search(secondPage).block();

        assertThat(nextResult).isNotNull();
        assertThat(nextResult.items())
                .extracting("paymentId")
                .containsExactly("pay_ops_old");
        assertThat(nextResult.nextPageToken()).isEmpty();
    }

    @Test
    void rejectsPageTokenWhenFiltersChange() {
        insertPayment("pay_ops_filter_old", "mer_ops_a", "cus_ops_a", "AUTHORIZED", BASE_TIME.plusSeconds(10));
        insertPayment("pay_ops_filter_mid", "mer_ops_a", "cus_ops_a", "AUTHORIZED", BASE_TIME.plusSeconds(20));
        insertPayment("pay_ops_filter_new", "mer_ops_a", "cus_ops_a", "AUTHORIZED", BASE_TIME.plusSeconds(30));

        var firstPage = OpsPaymentSearchRequest.firstPage(
                Optional.of(PaymentStatus.AUTHORIZED),
                Optional.of(MerchantId.of("mer_ops_a")),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                2
        );

        var result = adapter.search(firstPage).block();

        assertThat(result).isNotNull();
        assertThat(result.nextPageToken()).isPresent();

        var changedFilters = new OpsPaymentSearchRequest(
                Optional.of(PaymentStatus.AUTHORIZED),
                Optional.of(MerchantId.of("mer_ops_b")),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                2,
                result.nextPageToken()
        );

        assertThatThrownBy(() -> adapter.search(changedFilters).block())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("pageToken does not match current filters");
    }

    private void insertPayment(
            String paymentId,
            String merchantId,
            String customerId,
            String status,
            Instant createdAt
    ) {
        entityTemplate.insert(PaymentEntity.class)
                .using(PaymentEntity.builder()
                        .paymentId(paymentId)
                        .merchantId(merchantId)
                        .customerId(customerId)
                        .amountMinor(1299)
                        .currency("USD")
                        .paymentMethodTokenHash("token_hash_should_not_be_exposed")
                        .paymentMethodTokenLast4("1234")
                        .deviceFingerprintHash("device_hash_should_not_be_exposed")
                        .externalReference("order_" + paymentId)
                        .idempotencyKey("idem_" + paymentId)
                        .status(status)
                        .createdAt(createdAt)
                        .updatedAt(createdAt.plusSeconds(5))
                        .build())
                .block();
    }

    private void insertAuthorization(String paymentId, String status) {
        entityTemplate.insert(PaymentAuthorizationEntity.class)
                .using(PaymentAuthorizationEntity.builder()
                        .paymentAuthorizationId("pauth_" + paymentId)
                        .paymentId(paymentId)
                        .status(status)
                        .authorizationCode("AUTH-" + paymentId)
                        .requestedAt(BASE_TIME)
                        .riskPendingAt(BASE_TIME.plusSeconds(1))
                        .authorizedAt(BASE_TIME.plusSeconds(2))
                        .createdAt(BASE_TIME)
                        .updatedAt(BASE_TIME.plusSeconds(2))
                        .build())
                .block();
    }

    private void insertRiskDecision(String paymentId, String decision, int score) {
        entityTemplate.insert(PaymentRiskDecisionEntity.class)
                .using(PaymentRiskDecisionEntity.builder()
                        .paymentRiskDecisionId("risk_" + paymentId)
                        .paymentId(paymentId)
                        .decision(decision)
                        .score(score)
                        .reasonCodesJson("[\"LOW_RISK_PAYMENT\"]")
                        .ruleVersion("risk-rules-v1")
                        .decidedAt(BASE_TIME.plusSeconds(2))
                        .createdAt(BASE_TIME.plusSeconds(2))
                        .build())
                .block();
    }

    private void insertReversal(String paymentId, String status) {
        entityTemplate.insert(PaymentReversalEntity.class)
                .using(PaymentReversalEntity.builder()
                        .paymentReversalId("rev_" + paymentId)
                        .paymentId(paymentId)
                        .merchantId("mer_ops_a")
                        .customerId("cus_ops_a")
                        .idempotencyKey("idem_rev_" + paymentId)
                        .reason("merchant_requested")
                        .status(status)
                        .requestedAt(BASE_TIME.plusSeconds(3))
                        .reversedAt(BASE_TIME.plusSeconds(4))
                        .createdAt(BASE_TIME.plusSeconds(3))
                        .updatedAt(BASE_TIME.plusSeconds(4))
                        .build())
                .block();
    }
}
