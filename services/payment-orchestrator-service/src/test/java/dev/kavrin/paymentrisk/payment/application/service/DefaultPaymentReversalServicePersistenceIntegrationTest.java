package dev.kavrin.paymentrisk.payment.application.service;

import dev.kavrin.paymentrisk.TestPostgresConfiguration;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyScope;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyStatus;
import dev.kavrin.paymentrisk.idempotency.infrastructure.persistence.IdempotencyRecordEntityRepository;
import dev.kavrin.paymentrisk.payment.application.command.AuthorizePaymentCommand;
import dev.kavrin.paymentrisk.payment.application.command.ReversePaymentCommand;
import dev.kavrin.paymentrisk.payment.application.query.PaymentLookupService;
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
        DefaultPaymentReversalServicePersistenceIntegrationTest.PersistenceTestConfiguration.class
})
class DefaultPaymentReversalServicePersistenceIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-06-01T10:05:00Z");

    @Autowired
    private AuthorizePaymentService authorizePaymentService;

    @Autowired
    private PaymentReversalService paymentReversalService;

    @Autowired
    private PaymentLookupService paymentLookupService;

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
    void lookupAfterSuccessfulAuthorizationReturnsDurablePaymentDetails() {
        var authorization = authorizePaymentService.authorize(validAuthorizationCommand()).block();

        assertThat(authorization).isNotNull();

        var details = paymentLookupService.getPaymentDetails(
                dev.kavrin.paymentrisk.payment.domain.model.PaymentId.of(authorization.paymentId())
        ).block();

        assertThat(details).isNotNull();
        assertThat(details.paymentId()).isEqualTo(authorization.paymentId());
        assertThat(details.status()).isEqualTo("AUTHORIZED");
        assertThat(details.authorization().authorizationCode()).isEqualTo(authorization.authorizationCode());
        assertThat(details.risk().decision()).isEqualTo("APPROVED");
        assertThat(details.reversal()).isNull();
    }

    @Test
    void reversalAfterAuthorizationPersistsPaymentStateReversalOutboxAndCompletedIdempotencyRecord() {
        var authorization = authorizePaymentService.authorize(validAuthorizationCommand()).block();

        var reversal = paymentReversalService.reverse(validReversalCommand(authorization.paymentId())).block();

        assertThat(reversal).isNotNull();
        assertThat(reversal.paymentId()).isEqualTo(authorization.paymentId());
        assertThat(reversal.status()).isEqualTo("REVERSED");
        assertThat(reversal.reason()).isEqualTo("merchant_requested");

        var payment = paymentRepository.findById(authorization.paymentId()).block();
        assertThat(payment).isNotNull();
        assertThat(payment.getStatus()).isEqualTo("REVERSED");

        var reversalEntity = reversalRepository.findByPaymentId(authorization.paymentId()).block();
        assertThat(reversalEntity).isNotNull();
        assertThat(reversalEntity.getPaymentReversalId()).isEqualTo(reversal.reversalId());
        assertThat(reversalEntity.getIdempotencyKey()).isEqualTo("idem_reversal_123");

        var outboxEvents = outboxRepository.findAll().collectList().block();
        assertThat(outboxEvents)
                .isNotNull()
                .extracting(event -> event.getEventType())
                .containsExactlyInAnyOrder(
                        "PaymentAuthorizationRequested",
                        "PaymentAuthorized",
                        "PaymentReversed"
                );
        assertThat(outboxEvents.stream()
                .filter(event -> "PaymentReversed".equals(event.getEventType()))
                .findFirst())
                .get()
                .satisfies(event -> {
                    assertThat(event.getStatus()).isEqualTo("PENDING");
                    assertThat(event.getCorrelationId()).isEqualTo("corr-reversal-service");
                    assertThat(event.getPayloadJson())
                            .contains("\"paymentId\":\"" + authorization.paymentId() + "\"")
                            .contains("\"reversalId\":\"" + reversal.reversalId() + "\"");
                });

        var idempotencyRecords = idempotencyRepository.findAll().collectList().block();
        assertThat(idempotencyRecords).hasSize(2);
        assertThat(idempotencyRecords.stream()
                .filter(record -> IdempotencyScope.PAYMENT_REVERSAL.value().equals(record.getScope()))
                .findFirst())
                .get()
                .satisfies(record -> {
                    assertThat(record.getStatus()).isEqualTo(IdempotencyStatus.COMPLETED.name());
                    assertThat(record.getResponseStatus()).isEqualTo(200);
                    assertThat(record.getResponseBodyJson())
                            .contains("\"paymentId\":\"" + authorization.paymentId() + "\"")
                            .contains("\"reversalId\":\"" + reversal.reversalId() + "\"");
                });
    }

    @Test
    void duplicateReversalReturnsStoredResponseWithoutCreatingSecondReversalOrOutboxEvent() {
        var authorization = authorizePaymentService.authorize(validAuthorizationCommand()).block();

        var first = paymentReversalService.reverse(validReversalCommand(authorization.paymentId())).block();
        var second = paymentReversalService.reverse(validReversalCommand(authorization.paymentId())).block();

        assertThat(second).isEqualTo(first);
        assertThat(reversalRepository.count().block()).isOne();
        assertThat(outboxRepository.findAll()
                .filter(event -> "PaymentReversed".equals(event.getEventType()))
                .count()
                .block()).isOne();
        assertThat(idempotencyRepository.findAll()
                .filter(record -> IdempotencyScope.PAYMENT_REVERSAL.value().equals(record.getScope()))
                .count()
                .block()).isOne();
    }

    private static AuthorizePaymentCommand validAuthorizationCommand() {
        return new AuthorizePaymentCommand(
                "mer_01HX7Q9K2V6M8P4A3B9C1D2E3F",
                "cus_01HX7QAF4CQ8YFZ3M9N2W1P0VK",
                1299,
                "USD",
                "pmt_tok_4f7b8d9c2a1e",
                "dfp_6d9f1a2b3c4e5f678901",
                "order_2026_000123",
                "idem_authorization_123",
                "corr-authorization-service"
        );
    }

    private static ReversePaymentCommand validReversalCommand(String paymentId) {
        return new ReversePaymentCommand(
                paymentId,
                "idem_reversal_123",
                "merchant_requested",
                "corr-reversal-service"
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
