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
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
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
class DatabaseOutboxRelayEventReaderTest {

    private static final Instant NOW = Instant.parse("2026-06-05T08:00:00Z");

    @Autowired
    private DatabaseOutboxRelayEventReader reader;

    @Autowired
    private OutboxEventEntityRepository outboxRepository;

    @Autowired
    private R2dbcEntityTemplate entityTemplate;

    @Autowired
    private PostgreSQLContainer<?> postgresContainer;

    @BeforeEach
    void deleteExistingRecords() {
        outboxRepository.deleteAll().block();
    }

    @Test
    void shouldFindPendingAndRetryableFailedEventsOrderedByCreationTimeAndLimited() {
        insert(event("evt_published", "PUBLISHED", NOW.minusSeconds(40), NOW.minusSeconds(40), null));
        insert(event("evt_publishing", "PUBLISHING", NOW.minusSeconds(30), NOW.minusSeconds(30), NOW.minusSeconds(20)));
        insert(event("evt_failed_future", "FAILED", NOW.minusSeconds(20), NOW.plusSeconds(60), null));
        insert(event("evt_failed_ready", "FAILED", NOW.minusSeconds(10), NOW.minusSeconds(1), null));
        insert(event("evt_pending_old", "PENDING", NOW.minusSeconds(60), NOW.plusSeconds(300), null));
        insert(event("evt_pending_new", "PENDING", NOW.minusSeconds(5), null, null));

        var query = new OutboxRelayQuery(NOW, 2, true);

        var candidates = reader.findRelayCandidates(query).collectList().block();

        assertThat(candidates)
                .isNotNull()
                .extracting("eventId")
                .containsExactly("evt_pending_old", "evt_failed_ready");
    }

    @Test
    void shouldSkipRowsLockedByAnotherTransaction() throws Exception {
        insert(event("evt_locked", "PENDING", NOW.minusSeconds(60), NOW, null));
        insert(event("evt_available", "PENDING", NOW.minusSeconds(30), NOW, null));

        try (Connection connection = jdbcConnection()) {
            connection.setAutoCommit(false);
            lockOutboxRow(connection, "evt_locked");

            var query = new OutboxRelayQuery(NOW, 10, true);
            var candidates = reader.findRelayCandidates(query).collectList().block();

            assertThat(candidates)
                    .isNotNull()
                    .extracting("eventId")
                    .containsExactly("evt_available");

            connection.rollback();
        }
    }

    private Connection jdbcConnection() throws Exception {
        return DriverManager.getConnection(
                postgresContainer.getJdbcUrl(),
                postgresContainer.getUsername(),
                postgresContainer.getPassword()
        );
    }

    private void lockOutboxRow(Connection connection, String eventId) throws Exception {
        // Hold a real PostgreSQL row lock open. Reader query uses SKIP LOCKED,
        // so this row should be invisible to the relay candidate list.
        try (var statement = connection.prepareStatement(
                "SELECT event_id FROM outbox_events WHERE event_id = ? FOR UPDATE"
        )) {
            statement.setString(1, eventId);
            try (var resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
            }
        }
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
            Instant lockedAt
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
                .build();
    }
}
