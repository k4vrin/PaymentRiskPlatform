package dev.kavrin.paymentrisk.settlement.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kavrin.paymentrisk.TestPostgresConfiguration;
import dev.kavrin.paymentrisk.settlement.application.SettlementProjectionEvent;
import dev.kavrin.paymentrisk.settlement.domain.SettlementProjectionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

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
        DatabaseSettlementProjectionProjectorTest.SettlementProjectorTestConfiguration.class
})
class DatabaseSettlementProjectionProjectorTest {

    private static final Instant NOW = Instant.parse("2026-06-05T12:00:00Z");

    @Autowired
    private DatabaseSettlementProjectionProjector projector;

    @Autowired
    private SettlementProjectionRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void deleteExistingRecords() {
        repository.deleteAll().block();
    }

    @Test
    void shouldCreateSettlementReadyProjectionForAuthorizedPayment() throws Exception {
        StepVerifier.create(projector.project(event(
                        "evt_auth",
                        "PaymentAuthorized",
                        "2026-06-05T10:00:00Z"
                )))
                .verifyComplete();

        StepVerifier.create(repository.findByPaymentId("pay_123"))
                .assertNext(projection -> {
                    assertThat(projection.status()).isEqualTo(SettlementProjectionStatus.SETTLEMENT_READY.name());
                    assertThat(projection.paymentId()).isEqualTo("pay_123");
                    assertThat(projection.merchantId()).isEqualTo("mer_123");
                    assertThat(projection.customerId()).isEqualTo("cus_123");
                    assertThat(projection.amountMinor()).isEqualTo(1299);
                    assertThat(projection.currency()).isEqualTo("USD");
                    assertThat(projection.businessDate()).isEqualTo(LocalDate.parse("2026-06-05"));
                    assertThat(projection.authorizedAt()).isEqualTo(Instant.parse("2026-06-05T10:00:00Z"));
                    assertThat(projection.createdAt()).isEqualTo(NOW);
                    assertThat(projection.updatedAt()).isEqualTo(NOW);
                })
                .verifyComplete();
    }

    @Test
    void shouldCreateNotSettledProjectionForDeclinedPayment() throws Exception {
        StepVerifier.create(projector.project(event(
                        "evt_declined",
                        "PaymentDeclined",
                        "2026-06-05T10:01:00Z"
                )))
                .verifyComplete();

        StepVerifier.create(repository.findByPaymentId("pay_123"))
                .assertNext(projection -> {
                    assertThat(projection.status()).isEqualTo(SettlementProjectionStatus.NOT_SETTLED.name());
                    assertThat(projection.declinedAt()).isEqualTo(Instant.parse("2026-06-05T10:01:00Z"));
                    assertThat(projection.authorizedAt()).isNull();
                })
                .verifyComplete();
    }

    @Test
    void shouldMarkExistingProjectionReversed() throws Exception {
        StepVerifier.create(projector.project(event(
                        "evt_auth",
                        "PaymentAuthorized",
                        "2026-06-05T10:00:00Z"
                )))
                .verifyComplete();

        StepVerifier.create(projector.project(event(
                        "evt_reversal",
                        "PaymentReversed",
                        "2026-06-06T11:00:00Z"
                )))
                .verifyComplete();

        StepVerifier.create(repository.findByPaymentId("pay_123"))
                .assertNext(projection -> {
                    assertThat(projection.status()).isEqualTo(SettlementProjectionStatus.REVERSED.name());
                    assertThat(projection.lastEventId()).isEqualTo("evt_reversal");
                    assertThat(projection.lastEventType()).isEqualTo("PaymentReversed");
                    assertThat(projection.reversedAt()).isEqualTo(Instant.parse("2026-06-06T11:00:00Z"));
                    assertThat(projection.businessDate()).isEqualTo(LocalDate.parse("2026-06-06"));
                    assertThat(projection.merchantId()).isEqualTo("mer_123");
                })
                .verifyComplete();
    }

    @Test
    void shouldQueryByMerchantStatusAndBusinessDate() throws Exception {
        StepVerifier.create(projector.project(event(
                        "evt_auth",
                        "PaymentAuthorized",
                        "2026-06-05T10:00:00Z"
                )))
                .verifyComplete();

        StepVerifier.create(repository.findByMerchantIdAndStatusAndBusinessDateOrderByUpdatedAtDesc(
                        "mer_123",
                        SettlementProjectionStatus.SETTLEMENT_READY.name(),
                        LocalDate.parse("2026-06-05")
                ).collectList())
                .assertNext(projections -> assertThat(projections)
                        .extracting(SettlementProjectionEntity::paymentId)
                        .containsExactly("pay_123"))
                .verifyComplete();
    }

    private SettlementProjectionEvent event(
            String eventId,
            String eventType,
            String occurredAt
    ) throws Exception {
        return new SettlementProjectionEvent(
                eventId,
                eventType,
                "pay_123",
                "Payment",
                "1",
                "corr_123",
                Instant.parse(occurredAt),
                objectMapper.readTree("""
                        {
                          "paymentId": "pay_123",
                          "merchantId": "mer_123",
                          "customerId": "cus_123",
                          "amountMinor": 1299,
                          "currency": "USD"
                        }
                        """)
        );
    }

    @TestConfiguration
    static class SettlementProjectorTestConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
