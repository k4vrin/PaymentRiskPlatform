package dev.kavrin.paymentrisk.consumer.infrastructure.persistence;

import dev.kavrin.paymentrisk.TestPostgresConfiguration;
import dev.kavrin.paymentrisk.ops.infrastructure.deadletter.persistence.DeadLetterRecordEntityRepository;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.repository.OutboxEventEntityRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
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

import java.nio.charset.StandardCharsets;

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
        DatabaseKafkaDeadLetterRecorderTest.DeadLetterRecorderTestConfiguration.class
})
class DatabaseKafkaDeadLetterRecorderTest {

    @Autowired
    private DatabaseKafkaDeadLetterRecorder recorder;

    @Autowired
    private DeadLetterRecordEntityRepository deadLetterRepository;

    @Autowired
    private OutboxEventEntityRepository outboxRepository;

    @Autowired
    private MeterRegistry meterRegistry;

    @BeforeEach
    void deleteExistingRecords() {
        outboxRepository.deleteAll().block();
        deadLetterRepository.deleteAll().block();
        meterRegistry.clear();
    }

    @Test
    void shouldPersistKafkaPoisonMessageAndEmitDeadLetterOutboxEvent() {
        var record = new ConsumerRecord<>(
                "payment.authorization.completed",
                0,
                42,
                "pay_123",
                "{bad-json"
        );
        record.headers()
                .add(new RecordHeader("event_id", bytes("evt_123")))
                .add(new RecordHeader("correlation_id", bytes("corr_123")));

        StepVerifier.create(recorder.record(
                        "payment-audit-consumer",
                        record,
                        new IllegalArgumentException("bad payload")
                ))
                .verifyComplete();

        StepVerifier.create(deadLetterRepository.findByEventId("evt_123").collectList())
                .assertNext(deadLetters -> {
                    assertThat(deadLetters).hasSize(1);

                    var deadLetter = deadLetters.getFirst();
                    assertThat(deadLetter.getSourceSystem()).isEqualTo("KAFKA");
                    assertThat(deadLetter.getDestinationName()).isEqualTo("payment.authorization.completed");
                    assertThat(deadLetter.getKafkaPartition()).isZero();
                    assertThat(deadLetter.getKafkaOffset()).isEqualTo(42);
                    assertThat(deadLetter.getCorrelationId()).isEqualTo("corr_123");
                    assertThat(deadLetter.getHeadersJson()).contains("event_id");
                    assertThat(deadLetter.getPayloadPreview()).isEqualTo("{bad-json");
                })
                .verifyComplete();

        StepVerifier.create(outboxRepository.findAll().collectList())
                .assertNext(events -> {
                    assertThat(events).hasSize(1);
                    assertThat(events.getFirst().getEventType()).isEqualTo("DeadLetterRecorded");
                    assertThat(events.getFirst().getStatus()).isEqualTo("PENDING");
                    assertThat(events.getFirst().getPayloadJson()).contains("\"deadLetterId\"");
                })
                .verifyComplete();

        assertThat(meterRegistry.find("payment_risk_dead_letters_total").counter().count())
                .isEqualTo(1.0);
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    @TestConfiguration
    static class DeadLetterRecorderTestConfiguration {

        @Bean
        @Primary
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }
}
