package dev.kavrin.paymentrisk.ops.infrastructure.replay;

import dev.kavrin.paymentrisk.ops.application.replay.ReplayTarget;
import dev.kavrin.paymentrisk.ops.application.replay.ReplayTargetLookupPort;
import dev.kavrin.paymentrisk.ops.domain.ReplaySource;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class DatabaseReplayTargetLookupAdapter implements ReplayTargetLookupPort {

    private static final String OUTBOX_REPLAYABLE_STATUS = "FAILED";

    private final DatabaseClient databaseClient;

    @Override
    public Mono<ReplayTarget> findTarget(ReplaySource source, String targetId) {
        return switch (source) {
            case OUTBOX -> findOutboxTarget(targetId);
            case DEAD_LETTER -> findDeadLetterTarget(targetId);
        };
    }

    private Mono<ReplayTarget> findOutboxTarget(String eventId) {
        return databaseClient.sql("""
                        SELECT event_id, status
                        FROM outbox_events
                        WHERE event_id = :eventId
                        """)
                .bind("eventId", eventId)
                .map((row, metadata) -> {
                    var status = row.get("status", String.class);
                    return new ReplayTarget(
                            row.get("event_id", String.class),
                            OUTBOX_REPLAYABLE_STATUS.equals(status),
                            status
                    );
                })
                .one();
    }

    private Mono<ReplayTarget> findDeadLetterTarget(String deadLetterId) {
        return databaseClient.sql("""
                        SELECT dead_letter_id, status, replay_eligible
                        FROM dead_letter_records
                        WHERE dead_letter_id = :deadLetterId
                        """)
                .bind("deadLetterId", deadLetterId)
                .map((row, metadata) -> new ReplayTarget(
                        row.get("dead_letter_id", String.class),
                        Boolean.TRUE.equals(row.get("replay_eligible", Boolean.class)),
                        row.get("status", String.class)
                ))
                .one();
    }
}
