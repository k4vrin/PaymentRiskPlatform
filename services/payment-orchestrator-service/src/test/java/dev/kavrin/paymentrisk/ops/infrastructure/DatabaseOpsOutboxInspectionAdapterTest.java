package dev.kavrin.paymentrisk.ops.infrastructure;

import dev.kavrin.paymentrisk.TestPostgresConfiguration;
import dev.kavrin.paymentrisk.ops.application.OpsOutboxInspectionRequest;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.OutboxEventEntity;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.repository.OutboxEventEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Optional;

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
@Import(TestPostgresConfiguration.class)
class DatabaseOpsOutboxInspectionAdapterTest {

    private static final Instant BASE_TIME = Instant.parse("2026-06-04T10:00:00Z");

    @Autowired
    private DatabaseOpsOutboxInspectionAdapter adapter;

    @Autowired
    private OutboxEventEntityRepository outboxRepository;

    @Autowired
    private R2dbcEntityTemplate entityTemplate;

    @BeforeEach
    void deleteExistingRecords() {
        outboxRepository.deleteAll().block();
    }

    @Test
    void inspectsOutboxWithFiltersAndPayloadPreview() {
        insert(event("evt_001", "PaymentAuthorized", "pay_001", "PENDING", BASE_TIME.plusSeconds(30)));
        insert(event("evt_002", "PaymentDeclined", "pay_002", "PENDING", BASE_TIME.plusSeconds(20)));
        insert(event("evt_003", "PaymentAuthorized", "pay_003", "PUBLISHED", BASE_TIME.plusSeconds(10)));

        var request = new OpsOutboxInspectionRequest(
                Optional.of("PENDING"),
                Optional.of("PaymentAuthorized"),
                Optional.of("pay_001"),
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
        assertThat(item.eventId()).isEqualTo("evt_001");
        assertThat(item.aggregateId()).isEqualTo("pay_001");
        assertThat(item.aggregateType()).isEqualTo("PAYMENT");
        assertThat(item.eventType()).isEqualTo("PaymentAuthorized");
        assertThat(item.schemaVersion()).isEqualTo("v1");
        assertThat(item.status()).isEqualTo("PENDING");
        assertThat(item.retryCount()).isEqualTo(1);
        assertThat(item.correlationId()).contains("corr_evt_001");
        assertThat(item.payloadPreview()).contains("{\"eventId\":\"evt_001\"}");
    }

    @Test
    void ordersFailedEventsByNextRetryTime() {
        insert(failedEvent("evt_retry_later", BASE_TIME.plusSeconds(90), BASE_TIME.plusSeconds(30)));
        insert(failedEvent("evt_retry_soon", BASE_TIME.plusSeconds(10), BASE_TIME.plusSeconds(20)));
        insert(failedEvent("evt_retry_missing", null, BASE_TIME.plusSeconds(40)));

        var request = new OpsOutboxInspectionRequest(
                Optional.of("FAILED"),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                25,
                Optional.empty()
        );

        var result = adapter.inspect(request).block();

        assertThat(result).isNotNull();
        assertThat(result.items())
                .extracting("eventId")
                .containsExactly("evt_retry_soon", "evt_retry_later", "evt_retry_missing");
    }

    @Test
    void pagesOutboxResultsAndRejectsTokenWhenFiltersChange() {
        insert(event("evt_old", "PaymentAuthorized", "pay_old", "PENDING", BASE_TIME.plusSeconds(10)));
        insert(event("evt_mid", "PaymentAuthorized", "pay_mid", "PENDING", BASE_TIME.plusSeconds(20)));
        insert(event("evt_new", "PaymentAuthorized", "pay_new", "PENDING", BASE_TIME.plusSeconds(30)));

        var firstPage = new OpsOutboxInspectionRequest(
                Optional.of("PENDING"),
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
                .extracting("eventId")
                .containsExactly("evt_new", "evt_mid");
        assertThat(firstResult.nextPageToken()).isPresent();

        var secondPage = new OpsOutboxInspectionRequest(
                Optional.of("PENDING"),
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
                .extracting("eventId")
                .containsExactly("evt_old");

        var changedFilters = new OpsOutboxInspectionRequest(
                Optional.of("FAILED"),
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

    private void insert(OutboxEventEntity event) {
        entityTemplate.insert(OutboxEventEntity.class)
                .using(event)
                .block();
    }

    private static OutboxEventEntity failedEvent(
            String eventId,
            Instant nextRetryAt,
            Instant createdAt
    ) {
        return event(eventId, "PaymentAuthorized", "pay_failed", "FAILED", createdAt)
                .toBuilder()
                .retryCount(3)
                .nextRetryAt(nextRetryAt)
                .lastError("Kafka unavailable")
                .build();
    }

    private static OutboxEventEntity event(
            String eventId,
            String eventType,
            String aggregateId,
            String status,
            Instant createdAt
    ) {
        return OutboxEventEntity.builder()
                .eventId(eventId)
                .aggregateType("PAYMENT")
                .aggregateId(aggregateId)
                .eventType(eventType)
                .schemaVersion("v1")
                .producer("payment-orchestrator-service")
                .correlationId("corr_" + eventId)
                .payloadJson("{\"eventId\":\"" + eventId + "\"}")
                .status(status)
                .retryCount(1)
                .nextRetryAt(createdAt.plusSeconds(60))
                .occurredAt(createdAt.minusSeconds(1))
                .createdAt(createdAt)
                .publishedAt(status.equals("PUBLISHED") ? createdAt.plusSeconds(1) : null)
                .build();
    }
}
