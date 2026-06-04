package dev.kavrin.paymentrisk.ops.infrastructure.deadletter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.kavrin.paymentrisk.TestPostgresConfiguration;
import dev.kavrin.paymentrisk.ops.application.deadletter.OpsDeadLetterInspectionRequest;
import dev.kavrin.paymentrisk.ops.application.deadletter.ReplayEligibility;
import dev.kavrin.paymentrisk.ops.infrastructure.deadletter.persistence.DeadLetterRecordEntity;
import dev.kavrin.paymentrisk.ops.infrastructure.deadletter.persistence.DeadLetterRecordEntityRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.test.context.ActiveProfiles;

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
class DatabaseOpsDeadLetterInspectionAdapterTest {

    private static final Instant BASE_TIME = Instant.parse("2026-06-04T10:00:00Z");

    @Autowired
    private DatabaseOpsDeadLetterInspectionAdapter adapter;

    @Autowired
    private DeadLetterRecordEntityRepository repository;

    @Autowired
    private R2dbcEntityTemplate entityTemplate;

    @BeforeEach
    void deleteExistingRecords() {
        repository.deleteAll().block();
    }

    @Test
    void inspectsDeadLettersWithFilters() {
        insert(kafkaRecord("dlq_001", "RECORDED", "evt_001", "msg_001", BASE_TIME.plusSeconds(30)));
        insert(kafkaRecord("dlq_002", "DISCARDED", "evt_002", "msg_002", BASE_TIME.plusSeconds(20)));
        insert(rabbitRecord("dlq_003", "RECORDED", "msg_003", BASE_TIME.plusSeconds(10)));

        var request = new OpsDeadLetterInspectionRequest(
                Optional.of("KAFKA"),
                Optional.of("RECORDED"),
                Optional.of("payment.authorization.completed"),
                Optional.of("evt_001"),
                Optional.of("msg_001"),
                Optional.of(BASE_TIME),
                Optional.of(BASE_TIME.plusSeconds(60)),
                25,
                Optional.empty()
        );

        var result = adapter.inspect(request).block();

        assertThat(result).isNotNull();
        assertThat(result.items()).hasSize(1);
        assertThat(result.nextPageToken()).isEmpty();

        var item = result.items().getFirst();
        assertThat(item.deadLetterId()).isEqualTo("dlq_001");
        assertThat(item.sourceSystem()).isEqualTo("KAFKA");
        assertThat(item.destinationName()).isEqualTo("payment.authorization.completed");
        assertThat(item.status()).isEqualTo("RECORDED");
        assertThat(item.partition()).contains(3);
        assertThat(item.offset()).contains(42L);
        assertThat(item.eventId()).contains("evt_001");
        assertThat(item.messageId()).contains("msg_001");
        assertThat(item.failureReason()).isEqualTo("deserialization failed");
        assertThat(item.failedAt()).isEqualTo(BASE_TIME.plusSeconds(30));
        assertThat(item.replayEligibility()).isEqualTo(ReplayEligibility.ELIGIBLE);
        assertThat(item.correlationId()).contains("corr_dlq_001");
    }

    @Test
    void pagesDeadLettersAndRejectsTokenWhenFiltersChange() {
        insert(kafkaRecord("dlq_old", "RECORDED", "evt_old", "msg_old", BASE_TIME.plusSeconds(10)));
        insert(kafkaRecord("dlq_mid", "RECORDED", "evt_mid", "msg_mid", BASE_TIME.plusSeconds(20)));
        insert(kafkaRecord("dlq_new", "RECORDED", "evt_new", "msg_new", BASE_TIME.plusSeconds(30)));

        var firstPage = new OpsDeadLetterInspectionRequest(
                Optional.of("KAFKA"),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                2,
                Optional.empty()
        );

        var firstResult = adapter.inspect(firstPage).block();

        assertThat(firstResult).isNotNull();
        assertThat(firstResult.items())
                .extracting("deadLetterId")
                .containsExactly("dlq_new", "dlq_mid");
        assertThat(firstResult.nextPageToken()).isPresent();

        var secondPage = new OpsDeadLetterInspectionRequest(
                Optional.of("KAFKA"),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                2,
                firstResult.nextPageToken()
        );
        var secondResult = adapter.inspect(secondPage).block();

        assertThat(secondResult).isNotNull();
        assertThat(secondResult.items())
                .extracting("deadLetterId")
                .containsExactly("dlq_old");

        var changedFilters = new OpsDeadLetterInspectionRequest(
                Optional.of("RABBITMQ"),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                2,
                firstResult.nextPageToken()
        );

        assertThatThrownBy(() -> adapter.inspect(changedFilters).block())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("pageToken does not match current filters");
    }

    @Test
    void excludesPayloadPreviewFromInspectionReadModel() {
        insert(kafkaRecord("dlq_001", "RECORDED", "evt_001", "msg_001", BASE_TIME));

        var request = new OpsDeadLetterInspectionRequest(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                25,
                Optional.empty()
        );

        var result = adapter.inspect(request).block();

        assertThat(result).isNotNull();
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().deadLetterId()).isEqualTo("dlq_001");
    }

    private void insert(DeadLetterRecordEntity entity) {
        entityTemplate.insert(DeadLetterRecordEntity.class)
                .using(entity)
                .block();
    }

    private static DeadLetterRecordEntity kafkaRecord(
            String deadLetterId,
            String status,
            String eventId,
            String messageId,
            Instant failedAt
    ) {
        return DeadLetterRecordEntity.builder()
                .deadLetterId(deadLetterId)
                .sourceSystem("KAFKA")
                .destinationName("payment.authorization.completed")
                .kafkaPartition(3)
                .kafkaOffset(42L)
                .eventId(eventId)
                .messageId(messageId)
                .status(status)
                .failureReason("deserialization failed")
                .failedAt(failedAt)
                .replayEligible(true)
                .correlationId("corr_dlq_001")
                .payloadPreview("{\"sensitive\":\"payload\"}")
                .createdAt(failedAt)
                .build();
    }

    private static DeadLetterRecordEntity rabbitRecord(
            String deadLetterId,
            String status,
            String messageId,
            Instant failedAt
    ) {
        return DeadLetterRecordEntity.builder()
                .deadLetterId(deadLetterId)
                .sourceSystem("RABBITMQ")
                .destinationName("partner.callback.commands.dlq")
                .deliveryTag("delivery-003")
                .messageId(messageId)
                .status(status)
                .failureReason("partner callback failed")
                .failedAt(failedAt)
                .replayEligible(false)
                .correlationId("corr_dlq_003")
                .payloadPreview("{\"sensitive\":\"payload\"}")
                .createdAt(failedAt)
                .build();
    }
}
