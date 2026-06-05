package dev.kavrin.paymentrisk.outbox.infrastructure.persistence;

import dev.kavrin.paymentrisk.outbox.application.OutboxProducerRetryDecision;
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
    public Mono<Void> markFailure(
            String eventId,
            OutboxProducerRetryDecision decision,
            String errorMessage
    ) {
        var safeError = truncate(errorMessage);

        if (decision.retryable()) {
            return markRetryableFailure(
                    eventId,
                    decision,
                    safeError
            );
        }

        return markTerminalFailure(
                eventId,
                decision,
                safeError
        );
    }

    /**
     * Records a transient publish failure and schedules
     * the next relay attempt.
     */
    private Mono<Void> markRetryableFailure(
            String eventId,
            OutboxProducerRetryDecision decision,
            String errorMessage
    ) {
        return databaseClient.sql("""
                        UPDATE outbox_events
                        SET status = 'FAILED',
                            retry_count = :retryCount,
                            next_retry_at = :nextRetryAt,
                            last_error = :lastError,
                            locked_at = NULL,
                            relay_instance_id = NULL
                        WHERE event_id = :eventId
                        """)
                .bind("retryCount", decision.nextRetryCount())
                .bind("nextRetryAt", decision.nextRetryAt())
                .bind("lastError", errorMessage)
                .bind("eventId", eventId)
                .fetch()
                .rowsUpdated()
                .then();
    }

    /**
     * Records a terminal publish failure.
     * <p>
     * Terminal failures stay FAILED for ops visibility, but next_retry_at is
     * cleared so the relay query no longer picks them up automatically.
     */
    private Mono<Void> markTerminalFailure(
            String eventId,
            OutboxProducerRetryDecision decision,
            String errorMessage
    ) {
        return databaseClient.sql("""
                        UPDATE outbox_events
                        SET status = :status,
                            retry_count = :retryCount,
                            next_retry_at = NULL,
                            last_error = :lastError,
                            locked_at = NULL,
                            relay_instance_id = NULL
                        WHERE event_id = :eventId
                        """)
                .bind("status", decision.failureStatus())
                .bind("retryCount", decision.nextRetryCount())
                .bind("lastError", errorMessage)
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
