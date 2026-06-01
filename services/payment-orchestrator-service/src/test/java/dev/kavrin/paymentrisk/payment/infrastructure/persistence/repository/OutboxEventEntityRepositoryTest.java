package dev.kavrin.paymentrisk.payment.infrastructure.persistence.repository;

import dev.kavrin.paymentrisk.TestPostgresConfiguration;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.OutboxEventEntity;
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
class OutboxEventEntityRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-05-25T10:15:30Z");

    @Autowired
    private OutboxEventEntityRepository repository;

    @Autowired
    private R2dbcEntityTemplate entityTemplate;

    @BeforeEach
    void deleteExistingRecords() {
        repository.deleteAll().block();
    }

    @Test
    void savesPendingOutboxEvent() {
        OutboxEventEntity saved = entityTemplate.insert(OutboxEventEntity.class)
                .using(pendingEvent(
                "evt_01",
                "PaymentAuthorized",
                NOW
        )).block();

        assertThat(saved).isNotNull();
        assertThat(saved.getEventId()).isEqualTo("evt_01");

        OutboxEventEntity reloaded = repository.findById("evt_01").block();
        assertThat(reloaded).isNotNull();
        assertThat(reloaded.getAggregateType()).isEqualTo("PAYMENT");
        assertThat(reloaded.getAggregateId()).isEqualTo("pay_test");
        assertThat(reloaded.getEventType()).isEqualTo("PaymentAuthorized");
        assertThat(reloaded.getSchemaVersion()).isEqualTo("v1");
        assertThat(reloaded.getProducer()).isEqualTo("payment-orchestrator-service");
        assertThat(reloaded.getCorrelationId()).isEqualTo("corr-authorization-service");
        assertThat(reloaded.getPayloadJson()).contains("\"paymentId\":\"pay_test\"");
        assertThat(reloaded.getStatus()).isEqualTo("PENDING");
        assertThat(reloaded.getRetryCount()).isZero();
        assertThat(reloaded.getNextRetryAt()).isEqualTo(NOW);
        assertThat(reloaded.getOccurredAt()).isEqualTo(NOW);
        assertThat(reloaded.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void findsPendingEventsReadyForPublish() {
        insert(pendingEvent("evt_ready", "PaymentAuthorized", NOW.minusSeconds(1)));
        insert(pendingEvent("evt_future", "PaymentDeclined", NOW.plusSeconds(60)));
        insert(publishedEvent("evt_published", "PaymentAuthorized", NOW.minusSeconds(1)));

        var readyEvents = repository
                .findByStatusAndNextRetryAtLessThanEqual("PENDING", NOW)
                .collectList()
                .block();

        assertThat(readyEvents)
                .isNotNull()
                .extracting(OutboxEventEntity::getEventId)
                .containsExactly("evt_ready");
    }

    private static OutboxEventEntity pendingEvent(
            String eventId,
            String eventType,
            Instant nextRetryAt
    ) {
        return event(eventId, eventType, "PENDING", nextRetryAt, null);
    }

    private void insert(OutboxEventEntity event) {
        entityTemplate.insert(OutboxEventEntity.class)
                .using(event)
                .block();
    }

    private static OutboxEventEntity publishedEvent(
            String eventId,
            String eventType,
            Instant nextRetryAt
    ) {
        return event(eventId, eventType, "PUBLISHED", nextRetryAt, NOW);
    }

    private static OutboxEventEntity event(
            String eventId,
            String eventType,
            String status,
            Instant nextRetryAt,
            Instant publishedAt
    ) {
        return OutboxEventEntity.builder()
                .eventId(eventId)
                .aggregateType("PAYMENT")
                .aggregateId("pay_test")
                .eventType(eventType)
                .schemaVersion("v1")
                .producer("payment-orchestrator-service")
                .correlationId("corr-authorization-service")
                .payloadJson("{\"paymentId\":\"pay_test\"}")
                .status(status)
                .retryCount(0)
                .nextRetryAt(nextRetryAt)
                .occurredAt(NOW)
                .createdAt(NOW)
                .publishedAt(publishedAt)
                .build();
    }
}
