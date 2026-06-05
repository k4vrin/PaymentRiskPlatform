package dev.kavrin.paymentrisk.outbox.infrastructure.persistence;

import dev.kavrin.paymentrisk.TestPostgresConfiguration;
import dev.kavrin.paymentrisk.outbox.application.OutboxRelayQuery;
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
class DatabaseOutboxRelayClaimingTest {

    private static final Instant NOW = Instant.parse("2026-06-05T08:00:00Z");

    @Autowired
    private DatabaseOutboxRelayEventClaimer claimer;

    @Autowired
    private DatabaseOutboxRelayStatusUpdater statusUpdater;

    @Autowired
    private OutboxEventEntityRepository outboxRepository;

    @Autowired
    private R2dbcEntityTemplate entityTemplate;

    @BeforeEach
    void deleteExistingRecords() {
        outboxRepository.deleteAll().block();
    }

    @Test
    void shouldClaimEligibleEventsAndSkipAlreadyPublishingEvents() {
        insert(event("evt_pending", "PENDING", NOW.minusSeconds(30), NOW, null, null));
        insert(event("evt_failed_ready", "FAILED", NOW.minusSeconds(20), NOW.minusSeconds(1), null, null));
        insert(event("evt_failed_future", "FAILED", NOW.minusSeconds(10), NOW.plusSeconds(60), null, null));
        insert(event("evt_publishing", "PUBLISHING", NOW.minusSeconds(40), NOW, NOW, "other-relay"));

        var query = new OutboxRelayQuery(NOW, 10, true);

        var claimed = claimer.claimRelayCandidates(query, "relay-1")
                .collectList()
                .block();

        assertThat(claimed)
                .isNotNull()
                .extracting("eventId")
                .containsExactlyInAnyOrder("evt_pending", "evt_failed_ready");

        var pending = outboxRepository.findById("evt_pending").block();
        var failedReady = outboxRepository.findById("evt_failed_ready").block();
        var failedFuture = outboxRepository.findById("evt_failed_future").block();

        assertThat(pending).isNotNull();
        assertThat(pending.getStatus()).isEqualTo("PUBLISHING");
        assertThat(pending.getLockedAt()).isEqualTo(NOW);
        assertThat(pending.getRelayInstanceId()).isEqualTo("relay-1");

        assertThat(failedReady).isNotNull();
        assertThat(failedReady.getStatus()).isEqualTo("PUBLISHING");

        assertThat(failedFuture).isNotNull();
        assertThat(failedFuture.getStatus()).isEqualTo("FAILED");
    }

    @Test
    void shouldAvoidDoubleClaimingByStatusTransition() {
        insert(event("evt_once", "PENDING", NOW.minusSeconds(30), NOW, null, null));

        var query = new OutboxRelayQuery(NOW, 10, true);

        var firstClaim = claimer.claimRelayCandidates(query, "relay-1")
                .collectList()
                .block();
        var secondClaim = claimer.claimRelayCandidates(query, "relay-2")
                .collectList()
                .block();

        assertThat(firstClaim)
                .isNotNull()
                .extracting("eventId")
                .containsExactly("evt_once");
        assertThat(secondClaim).isEmpty();
    }

    @Test
    void shouldMarkPublishedAndReleaseClaim() {
        insert(event("evt_published", "PUBLISHING", NOW.minusSeconds(30), NOW, NOW, "relay-1"));

        statusUpdater.markPublished("evt_published").block();

        var event = outboxRepository.findById("evt_published").block();

        assertThat(event).isNotNull();
        assertThat(event.getStatus()).isEqualTo("PUBLISHED");
        assertThat(event.getPublishedAt()).isNotNull();
        assertThat(event.getLockedAt()).isNull();
        assertThat(event.getRelayInstanceId()).isNull();
    }

    @Test
    void shouldMarkFailedAndPreservePayloadForRetry() {
        insert(event("evt_failed", "PUBLISHING", NOW.minusSeconds(30), NOW, NOW, "relay-1"));

        statusUpdater.markFailed("evt_failed", "Kafka unavailable").block();

        var event = outboxRepository.findById("evt_failed").block();

        assertThat(event).isNotNull();
        assertThat(event.getStatus()).isEqualTo("FAILED");
        assertThat(event.getLastError()).isEqualTo("Kafka unavailable");
        assertThat(event.getPayloadJson()).isEqualTo("{\"eventId\":\"evt_failed\"}");
        assertThat(event.getLockedAt()).isNull();
        assertThat(event.getRelayInstanceId()).isNull();
    }

    private void insert(OutboxEventEntity event) {
        entityTemplate.insert(OutboxEventEntity.class)
                .using(event)
                .block();
    }

    private static OutboxEventEntity event(
            String eventId,
            String status,
            Instant createdAt,
            Instant nextRetryAt,
            Instant lockedAt,
            String relayInstanceId
    ) {
        return OutboxEventEntity.builder()
                .eventId(eventId)
                .aggregateType("PAYMENT")
                .aggregateId("pay_" + eventId)
                .eventType("PaymentAuthorized")
                .schemaVersion("v1")
                .producer("payment-orchestrator-service")
                .correlationId("corr_" + eventId)
                .payloadJson("{\"eventId\":\"" + eventId + "\"}")
                .status(status)
                .retryCount(status.equals("FAILED") ? 2 : 0)
                .nextRetryAt(nextRetryAt)
                .lastError(status.equals("FAILED") ? "Kafka unavailable" : null)
                .occurredAt(createdAt.minusSeconds(1))
                .createdAt(createdAt)
                .publishedAt(status.equals("PUBLISHED") ? createdAt.plusSeconds(1) : null)
                .lockedAt(lockedAt)
                .relayInstanceId(relayInstanceId)
                .build();
    }
}
