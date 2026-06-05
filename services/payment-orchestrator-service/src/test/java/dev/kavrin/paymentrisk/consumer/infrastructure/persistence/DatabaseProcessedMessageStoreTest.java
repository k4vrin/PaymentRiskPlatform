package dev.kavrin.paymentrisk.consumer.infrastructure.persistence;

import dev.kavrin.paymentrisk.TestPostgresConfiguration;
import dev.kavrin.paymentrisk.consumer.application.ProcessedMessageCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

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
        DatabaseProcessedMessageStoreTest.ProcessedMessageStoreTestConfiguration.class
})
class DatabaseProcessedMessageStoreTest {

    private static final Instant NOW = Instant.parse("2026-06-05T08:30:00Z");

    @Autowired
    private DatabaseProcessedMessageStore store;

    @Autowired
    private ProcessedKafkaMessageEntityRepository repository;

    @BeforeEach
    void deleteExistingRecords() {
        repository.deleteAll().block();
    }

    @Test
    void shouldRecordProcessedMessageWithKafkaPositionAndTimestamp() {
        var recorded = store.recordProcessed(command("payments-audit", "evt_1", 0, 42))
                .block();

        assertThat(recorded).isTrue();
        assertThat(store.isProcessed("payments-audit", "evt_1").block()).isTrue();

        var entity = repository.findById("payments-audit:evt_1").block();

        assertThat(entity).isNotNull();
        assertThat(entity.getConsumerName()).isEqualTo("payments-audit");
        assertThat(entity.getTopic()).isEqualTo("payment.authorization.completed");
        assertThat(entity.getPartition()).isZero();
        assertThat(entity.getOffset()).isEqualTo(42);
        assertThat(entity.getEventId()).isEqualTo("evt_1");
        assertThat(entity.getProcessedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldReturnFalseWhenConsumerAlreadyProcessedEvent() {
        var first = store.recordProcessed(command("payments-audit", "evt_1", 0, 42))
                .block();
        var duplicate = store.recordProcessed(command("payments-audit", "evt_1", 0, 43))
                .block();

        assertThat(first).isTrue();
        assertThat(duplicate).isFalse();
        assertThat(repository.count().block()).isOne();
    }

    @Test
    void shouldReturnFalseWhenConsumerAlreadyProcessedKafkaPosition() {
        var first = store.recordProcessed(command("payments-audit", "evt_1", 0, 42))
                .block();
        var duplicatePosition = store.recordProcessed(command("payments-audit", "evt_2", 0, 42))
                .block();

        assertThat(first).isTrue();
        assertThat(duplicatePosition).isFalse();
        assertThat(repository.count().block()).isOne();
    }

    private static ProcessedMessageCommand command(
            String consumerName,
            String eventId,
            int partition,
            long offset
    ) {
        return new ProcessedMessageCommand(
                consumerName,
                "payment.authorization.completed",
                partition,
                offset,
                eventId
        );
    }

    @TestConfiguration
    static class ProcessedMessageStoreTestConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
