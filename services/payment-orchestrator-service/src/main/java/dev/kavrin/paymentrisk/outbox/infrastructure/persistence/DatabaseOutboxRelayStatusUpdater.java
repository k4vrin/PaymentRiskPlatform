package dev.kavrin.paymentrisk.outbox.infrastructure.persistence;

import dev.kavrin.paymentrisk.outbox.application.OutboxRelayStatusUpdater;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Repository
@RequiredArgsConstructor
public class DatabaseOutboxRelayStatusUpdater implements OutboxRelayStatusUpdater {

    private final DatabaseClient databaseClient;

    @Override
    public Mono<Void> markPublished(String eventId) {
        var now = Instant.now();

        return databaseClient.sql("""
                        UPDATE outbox_events
                        SET status = 'PUBLISHED',
                            published_at = :now,
                            locked_at = NULL,
                            relay_instance_id = NULL
                        WHERE event_id = :eventId
                        """)
                .bind("now", now)
                .bind("eventId", eventId)
                .fetch()
                .rowsUpdated()
                .then();
    }

    @Override
    public Mono<Void> markFailed(String eventId, String errorMessage) {
        var safeError = truncate(errorMessage);

        return databaseClient.sql("""
                        UPDATE outbox_events
                        SET status = 'FAILED',
                            last_error = :lastError,
                            locked_at = NULL,
                            relay_instance_id = NULL
                        WHERE event_id = :eventId
                        """)
                .bind("lastError", safeError)
                .bind("eventId", eventId)
                .fetch()
                .rowsUpdated()
                .then();
    }

    private static String truncate(String value) {
        if (value == null || value.isBlank()) {
            return "Unknown publish failure";
        }

        return value.length() <= 500 ? value : value.substring(0, 500);
    }
}
