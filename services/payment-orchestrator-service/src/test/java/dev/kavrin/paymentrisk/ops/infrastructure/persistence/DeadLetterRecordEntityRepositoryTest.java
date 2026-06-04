package dev.kavrin.paymentrisk.ops.infrastructure.persistence;

import dev.kavrin.paymentrisk.TestPostgresConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

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
@Import(TestPostgresConfiguration.class)
class DeadLetterRecordEntityRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-06-04T10:00:00Z");

    @Autowired
    private DeadLetterRecordEntityRepository repository;

    @Autowired
    private R2dbcEntityTemplate entityTemplate;

    @BeforeEach
    void deleteExistingRecords() {
        repository.deleteAll().block();
    }

    @Test
    void savesAndReadsDeadLetterRecord() {
        insert(kafkaRecord("dlq_001", "RECORDED", NOW));

        var reloaded = repository.findById("dlq_001").block();

        assertThat(reloaded).isNotNull();
        assertThat(reloaded.getSourceSystem()).isEqualTo("KAFKA");
        assertThat(reloaded.getDestinationName()).isEqualTo("payment.authorization.completed");
        assertThat(reloaded.getKafkaPartition()).isEqualTo(3);
        assertThat(reloaded.getKafkaOffset()).isEqualTo(42L);
        assertThat(reloaded.getEventId()).isEqualTo("evt_001");
        assertThat(reloaded.getStatus()).isEqualTo("RECORDED");
        assertThat(reloaded.isReplayEligible()).isTrue();
        assertThat(reloaded.getCorrelationId()).isEqualTo("corr_dlq_001");
    }

    @Test
    void filtersBySourceAndStatus() {
        insert(kafkaRecord("dlq_001", "RECORDED", NOW));
        insert(kafkaRecord("dlq_002", "DISCARDED", NOW.plusSeconds(1)));
        insert(rabbitRecord("dlq_003", "RECORDED", NOW.plusSeconds(2)));

        var records = repository
                .findBySourceSystemAndStatusOrderByFailedAtDesc("KAFKA", "RECORDED")
                .collectList()
                .block();

        assertThat(records)
                .isNotNull()
                .extracting(DeadLetterRecordEntity::getDeadLetterId)
                .containsExactly("dlq_001");
    }

    @Test
    void filtersByEventAndMessageIds() {
        insert(kafkaRecord("dlq_001", "RECORDED", NOW));
        insert(rabbitRecord("dlq_002", "RECORDED", NOW.plusSeconds(1)));

        var byEvent = repository.findByEventId("evt_001").collectList().block();
        var byMessage = repository.findByMessageId("msg_002").collectList().block();

        assertThat(byEvent)
                .isNotNull()
                .extracting(DeadLetterRecordEntity::getDeadLetterId)
                .containsExactly("dlq_001");
        assertThat(byMessage)
                .isNotNull()
                .extracting(DeadLetterRecordEntity::getDeadLetterId)
                .containsExactly("dlq_002");
    }

    private void insert(DeadLetterRecordEntity entity) {
        entityTemplate.insert(DeadLetterRecordEntity.class)
                .using(entity)
                .block();
    }

    private static DeadLetterRecordEntity kafkaRecord(
            String deadLetterId,
            String status,
            Instant failedAt
    ) {
        return DeadLetterRecordEntity.builder()
                .deadLetterId(deadLetterId)
                .sourceSystem("KAFKA")
                .destinationName("payment.authorization.completed")
                .kafkaPartition(3)
                .kafkaOffset(42L)
                .eventId("evt_001")
                .messageId("msg_001")
                .status(status)
                .failureReason("deserialization failed")
                .failedAt(failedAt)
                .replayEligible(true)
                .correlationId("corr_dlq_001")
                .payloadPreview("{\"eventId\":\"evt_001\"}")
                .createdAt(failedAt)
                .build();
    }

    private static DeadLetterRecordEntity rabbitRecord(
            String deadLetterId,
            String status,
            Instant failedAt
    ) {
        return DeadLetterRecordEntity.builder()
                .deadLetterId(deadLetterId)
                .sourceSystem("RABBITMQ")
                .destinationName("partner.callback.commands.dlq")
                .deliveryTag("delivery-002")
                .messageId("msg_002")
                .status(status)
                .failureReason("partner callback failed")
                .failedAt(failedAt)
                .replayEligible(false)
                .correlationId("corr_dlq_002")
                .payloadPreview("{\"messageId\":\"msg_002\"}")
                .createdAt(failedAt)
                .build();
    }
}
