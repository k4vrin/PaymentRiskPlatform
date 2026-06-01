package dev.kavrin.paymentrisk.payment.application.service;

import dev.kavrin.paymentrisk.TestPostgresConfiguration;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyStatus;
import dev.kavrin.paymentrisk.idempotency.infrastructure.persistence.IdempotencyRecordEntityRepository;
import dev.kavrin.paymentrisk.payment.application.command.AuthorizePaymentCommand;
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
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

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
@Import({
        TestPostgresConfiguration.class,
        DefaultAuthorizePaymentServicePersistenceIntegrationTest.PersistenceTestConfiguration.class
})
class DefaultAuthorizePaymentServicePersistenceIntegrationTest {

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

    @BeforeEach
    void deleteExistingRecords() {
        outboxRepository.deleteAll().block();
        riskDecisionRepository.deleteAll().block();
        authorizationRepository.deleteAll().block();
        idempotencyRepository.deleteAll().block();
        paymentRepository.deleteAll().block();
    }

    @Test
    void authorizationSuccessPersistsPaymentStateOutboxEventsAndCompletedIdempotencyRecord() {
        var result = service.authorize(validCommand()).block();

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo("AUTHORIZED");
        assertThat(result.authorizationCode()).isNotBlank();
        assertThat(result.riskDecision()).isEqualTo("APPROVED");

        assertThat(paymentRepository.count().block()).isOne();
        assertThat(authorizationRepository.count().block()).isOne();
        assertThat(riskDecisionRepository.count().block()).isOne();
        assertThat(outboxRepository.count().block()).isEqualTo(2);

        var idempotencyRecords = idempotencyRepository.findAll()
                .collectList()
                .block();

        assertThat(idempotencyRecords).hasSize(1);
        assertThat(idempotencyRecords.getFirst().getStatus())
                .isEqualTo(IdempotencyStatus.COMPLETED.name());
        assertThat(idempotencyRecords.getFirst().getResponseStatus()).isEqualTo(200);
        assertThat(idempotencyRecords.getFirst().getResponseBodyJson())
                .contains("\"paymentId\":\"" + result.paymentId() + "\"")
                .contains("\"status\":\"AUTHORIZED\"");
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
    static class PersistenceTestConfiguration {

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
    }
}
