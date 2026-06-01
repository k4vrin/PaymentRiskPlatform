package dev.kavrin.paymentrisk.payment.application.service;

import dev.kavrin.paymentrisk.TestPostgresConfiguration;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyStatus;
import dev.kavrin.paymentrisk.idempotency.infrastructure.persistence.IdempotencyRecordEntityRepository;
import dev.kavrin.paymentrisk.payment.application.command.AuthorizePaymentCommand;
import dev.kavrin.paymentrisk.payment.application.outbox.PaymentOutboxEventWriter;
import dev.kavrin.paymentrisk.payment.domain.model.Payment;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.repository.OutboxEventEntityRepository;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.repository.PaymentAuthorizationEntityRepository;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.repository.PaymentEntityRepository;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.repository.PaymentRiskDecisionEntityRepository;
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
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
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
        DefaultAuthorizePaymentServiceTransactionTest.TransactionTestConfiguration.class
})
class DefaultAuthorizePaymentServiceTransactionTest {

    private static final Instant NOW = Instant.parse("2026-05-25T10:15:30Z");

    @Autowired
    private AuthorizePaymentService service;

    @Autowired
    private PaymentEntityRepository paymentRepository;

    @Autowired
    private PaymentAuthorizationEntityRepository authorizationRepository;

    @Autowired
    private PaymentRiskDecisionEntityRepository riskDecisionRepository;

    @Autowired
    private OutboxEventEntityRepository outboxRepository;

    @Autowired
    private IdempotencyRecordEntityRepository idempotencyRepository;

    @Autowired
    private ReactiveTransactionManager reactiveTransactionManager;

    @Autowired
    private TransactionalOperator transactionalOperator;

    @BeforeEach
    void deleteExistingRecords() {
        outboxRepository.deleteAll().block();
        riskDecisionRepository.deleteAll().block();
        authorizationRepository.deleteAll().block();
        idempotencyRepository.deleteAll().block();
        paymentRepository.deleteAll().block();
    }

    @Test
    void contextProvidesReactiveTransactionBeans() {
        assertThat(reactiveTransactionManager).isNotNull();
        assertThat(transactionalOperator).isNotNull();
    }

    @Test
    void outboxFailureRollsBackPaymentWritesAndLeavesFailedIdempotencyRecord() {
        assertThatThrownBy(() -> service.authorize(validCommand()).block())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("outbox insert failed");

        assertThat(paymentRepository.count().block()).isZero();
        assertThat(authorizationRepository.count().block()).isZero();
        assertThat(riskDecisionRepository.count().block()).isZero();
        assertThat(outboxRepository.count().block()).isZero();

        var idempotencyRecords = idempotencyRepository.findAll()
                .collectList()
                .block();

        assertThat(idempotencyRecords).hasSize(1);
        assertThat(idempotencyRecords.getFirst().getStatus())
                .isEqualTo(IdempotencyStatus.FAILED.name());
        assertThat(idempotencyRecords.getFirst().getResponseBodyJson()).isNull();
        assertThat(idempotencyRecords.getFirst().getResponseStatus()).isNull();
    }

    private static AuthorizePaymentCommand validCommand() {
        return new AuthorizePaymentCommand(
                "mer_01HX7Q9K2V6M8P4A3B9C1D2E3F",
                "cus_01HX7QAF4CQ8YFZ3M9N2W1P0VK",
                1299,
                "USD",
                "pmt_tok_4f7b8d9c2a1e",
                "dfp_6d9f1a2b3c4e5f678901",
                "order_2026_000123",
                "idem_01HX7QK9JP7E5W5NRZ6T5Q3R1A",
                "corr-authorization-service"
        );
    }

    @TestConfiguration
    static class TransactionTestConfiguration {

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
        PaymentOutboxEventWriter failingOutboxEventWriter() {
            return new PaymentOutboxEventWriter() {
                @Override
                public Mono<Void> writeAuthorizationEvents(
                        Payment payment,
                        String correlationId
                ) {
                    return Mono.error(new IllegalStateException("outbox insert failed"));
                }
            };
        }
    }
}
