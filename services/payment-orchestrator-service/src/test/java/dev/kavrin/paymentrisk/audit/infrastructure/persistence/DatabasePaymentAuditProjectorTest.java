package dev.kavrin.paymentrisk.audit.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kavrin.paymentrisk.TestPostgresConfiguration;
import dev.kavrin.paymentrisk.audit.application.PaymentAuditProjection;
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
        DatabasePaymentAuditProjectorTest.AuditProjectorTestConfiguration.class
})
class DatabasePaymentAuditProjectorTest {

    private static final Instant NOW = Instant.parse("2026-06-05T12:00:00Z");

    @Autowired
    private DatabasePaymentAuditProjector projector;

    @Autowired
    private PaymentAuditEventRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void deleteExistingRecords() {
        repository.deleteAll().block();
    }

    @Test
    void shouldPersistPaymentAuditProjection() throws Exception {
        var payload = objectMapper.readTree("""
                {
                  "paymentId": "pay_123",
                  "status": "AUTHORIZED"
                }
                """);

        var projection = new PaymentAuditProjection(
                "evt_123",
                "PaymentAuthorized",
                "pay_123",
                "Payment",
                "1",
                "corr_123",
                Instant.parse("2026-06-05T10:00:00Z"),
                payload
        );

        StepVerifier.create(projector.project(projection))
                .verifyComplete();

        StepVerifier.create(repository.findByPaymentIdOrderByOccurredAtAsc("pay_123").collectList())
                .assertNext(events -> {
                    assertThat(events).hasSize(1);

                    var event = events.getFirst();

                    assertThat(event.eventId()).isEqualTo("evt_123");
                    assertThat(event.eventType()).isEqualTo("PaymentAuthorized");
                    assertThat(event.paymentId()).isEqualTo("pay_123");
                    assertThat(event.aggregateType()).isEqualTo("Payment");
                    assertThat(event.schemaVersion()).isEqualTo("1");
                    assertThat(event.correlationId()).isEqualTo("corr_123");
                    assertThat(event.occurredAt()).isEqualTo(Instant.parse("2026-06-05T10:00:00Z"));
                    assertThat(event.createdAt()).isEqualTo(NOW);
                    assertThat(event.payloadJson()).contains("\"status\":\"AUTHORIZED\"");
                })
                .verifyComplete();
    }

    @Test
    void shouldReadAuditHistoryByPaymentInOccurrenceOrder() throws Exception {
        StepVerifier.create(projector.project(projection("evt_2", "PaymentReversed", "2026-06-05T10:05:00Z")))
                .verifyComplete();
        StepVerifier.create(projector.project(projection("evt_1", "PaymentAuthorized", "2026-06-05T10:00:00Z")))
                .verifyComplete();

        StepVerifier.create(repository.findByPaymentIdOrderByOccurredAtAsc("pay_123").collectList())
                .assertNext(events -> assertThat(events)
                        .extracting(PaymentAuditEventEntity::eventId)
                        .containsExactly("evt_1", "evt_2"))
                .verifyComplete();
    }

    private PaymentAuditProjection projection(
            String eventId,
            String eventType,
            String occurredAt
    ) throws Exception {
        return new PaymentAuditProjection(
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
                          "status": "AUTHORIZED"
                        }
                        """)
        );
    }

    @TestConfiguration
    static class AuditProjectorTestConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
