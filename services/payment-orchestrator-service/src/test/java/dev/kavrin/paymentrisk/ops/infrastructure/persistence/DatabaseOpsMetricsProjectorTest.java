package dev.kavrin.paymentrisk.ops.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kavrin.paymentrisk.TestPostgresConfiguration;
import dev.kavrin.paymentrisk.ops.application.metrics.OpsMetricsEvent;
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
        DatabaseOpsMetricsProjectorTest.OpsMetricsProjectorTestConfiguration.class
})
class DatabaseOpsMetricsProjectorTest {

    private static final Instant NOW = Instant.parse("2026-06-05T12:00:00Z");

    @Autowired
    private DatabaseOpsMetricsProjector projector;

    @Autowired
    private OpsEventMetricRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void deleteExistingRecords() {
        repository.deleteAll().block();
    }

    @Test
    void shouldIncrementEventAndBusinessCounters() throws Exception {
        StepVerifier.create(projector.project(event("evt_1", "PaymentAuthorized")))
                .verifyComplete();
        StepVerifier.create(projector.project(event("evt_2", "PaymentAuthorized")))
                .verifyComplete();

        StepVerifier.create(repository.findByMetricKey("payments.authorized"))
                .assertNext(metric -> {
                    assertThat(metric.metricValue()).isEqualTo(2);
                    assertThat(metric.lastEventId()).isEqualTo("evt_2");
                    assertThat(metric.lastObservedAt()).isEqualTo(Instant.parse("2026-06-05T10:00:00Z"));
                    assertThat(metric.createdAt()).isEqualTo(NOW);
                    assertThat(metric.updatedAt()).isEqualTo(NOW);
                })
                .verifyComplete();

        StepVerifier.create(repository.findByMetricKey("events.total"))
                .assertNext(metric -> assertThat(metric.metricValue()).isEqualTo(2))
                .verifyComplete();
    }

    @Test
    void shouldTrackDeadLetterRecordedCounter() throws Exception {
        StepVerifier.create(projector.project(event("evt_dlq", "DeadLetterRecorded")))
                .verifyComplete();

        StepVerifier.create(repository.findByMetricKey("dead_letters.recorded"))
                .assertNext(metric -> assertThat(metric.metricValue()).isOne())
                .verifyComplete();
    }

    private OpsMetricsEvent event(String eventId, String eventType) throws Exception {
        return new OpsMetricsEvent(
                eventId,
                eventType,
                "pay_123",
                "Payment",
                "1",
                "corr_123",
                Instant.parse("2026-06-05T10:00:00Z"),
                objectMapper.readTree("{\"paymentId\":\"pay_123\"}")
        );
    }

    @TestConfiguration
    static class OpsMetricsProjectorTestConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
