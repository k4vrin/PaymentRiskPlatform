package dev.kavrin.paymentrisk.payment.application.service;

import dev.kavrin.paymentrisk.TestPostgresConfiguration;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyScope;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyStatus;
import dev.kavrin.paymentrisk.idempotency.infrastructure.persistence.IdempotencyRecordEntityRepository;
import dev.kavrin.paymentrisk.payment.application.command.ReversePaymentCommand;
import dev.kavrin.paymentrisk.payment.application.outbox.PaymentOutboxEventWriter;
import dev.kavrin.paymentrisk.payment.domain.model.*;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.repository.*;
import dev.kavrin.paymentrisk.risk.application.RiskScoringClient;
import dev.kavrin.paymentrisk.risk.application.dto.RiskScoringResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

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
@Import({
        TestPostgresConfiguration.class,
        DefaultPaymentReversalServiceRollbackIntegrationTest.RollbackTestConfiguration.class
})
class DefaultPaymentReversalServiceRollbackIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-06-01T10:05:00Z");

    @Autowired
    private PaymentReversalService paymentReversalService;

    @Autowired
    private PaymentStatePersistencePort paymentStatePersistence;

    @Autowired
    private PaymentEntityRepository paymentRepository;

    @Autowired
    private PaymentAuthorizationEntityRepository authorizationRepository;

    @Autowired
    private PaymentRiskDecisionEntityRepository riskDecisionRepository;

    @Autowired
    private PaymentReversalEntityRepository reversalRepository;

    @Autowired
    private OutboxEventEntityRepository outboxRepository;

    @Autowired
    private IdempotencyRecordEntityRepository idempotencyRepository;

    @BeforeEach
    void deleteExistingRecords() {
        outboxRepository.deleteAll().block();
        reversalRepository.deleteAll().block();
        riskDecisionRepository.deleteAll().block();
        authorizationRepository.deleteAll().block();
        idempotencyRepository.deleteAll().block();
        paymentRepository.deleteAll().block();
    }

    @Test
    void rollsBackPaymentAndReversalRowsWhenReversalOutboxInsertFails() {
        paymentStatePersistence.save(authorizedPayment()).block();

        assertThatThrownBy(() -> paymentReversalService.reverse(validReversalCommand()).block())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("outbox insert failed");

        var payment = paymentRepository.findById("pay_rollback").block();
        assertThat(payment).isNotNull();
        assertThat(payment.getStatus()).isEqualTo("AUTHORIZED");
        assertThat(reversalRepository.count().block()).isZero();
        assertThat(outboxRepository.count().block()).isZero();

        var reversalIdempotency = idempotencyRepository
                .findByScopeAndIdempotencyKey(
                        IdempotencyScope.PAYMENT_REVERSAL.value(),
                        "idem_reversal_rollback"
                )
                .block();
        assertThat(reversalIdempotency).isNotNull();
        assertThat(reversalIdempotency.getStatus()).isEqualTo(IdempotencyStatus.FAILED.name());
        assertThat(reversalIdempotency.getExpiresAt()).isEqualTo(NOW);
        assertThat(reversalIdempotency.getResponseBodyJson()).isNull();
    }

    private static ReversePaymentCommand validReversalCommand() {
        return new ReversePaymentCommand(
                "pay_rollback",
                "idem_reversal_rollback",
                "merchant_requested",
                "corr-reversal-rollback"
        );
    }

    private static Payment authorizedPayment() {
        Payment payment = Payment.newAuthorizationAttempt(
                PaymentId.of("pay_rollback"),
                MerchantId.of("mer_01HX7Q9K2V6M8P4A3B9C1D2E3F"),
                CustomerId.of("cus_01HX7QAF4CQ8YFZ3M9N2W1P0VK"),
                Money.of(1299, "USD"),
                PaymentMethodToken.of("pmt_tok_4f7b8d9c2a1e"),
                DeviceFingerprint.of("dfp_6d9f1a2b3c4e5f678901"),
                ExternalReference.of("order_2026_000123"),
                IdempotencyKey.of("idem_authorization_rollback"),
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

    @TestConfiguration
    static class RollbackTestConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }

        @Bean
        @Primary
        RiskScoringClient approvedRiskScoringClient() {
            return request -> Mono.just(RiskScoringResponse.approved(
                    7,
                    List.of("LOW_RISK"),
                    "risk-rules-v1"
            ));
        }

        @Bean
        @Primary
        PaymentOutboxEventWriter failingPaymentOutboxEventWriter() {
            return new PaymentOutboxEventWriter() {
                @Override
                public Mono<Void> writeAuthorizationEvents(
                        Payment payment,
                        String correlationId
                ) {
                    return Mono.empty();
                }

                @Override
                public Mono<Void> writePaymentReversedEvents(
                        Payment payment,
                        String correlationId
                ) {
                    return Mono.error(new IllegalStateException("outbox insert failed"));
                }
            };
        }
    }
}
