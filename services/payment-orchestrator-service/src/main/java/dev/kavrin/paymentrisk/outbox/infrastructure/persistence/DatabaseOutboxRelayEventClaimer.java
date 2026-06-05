package dev.kavrin.paymentrisk.outbox.infrastructure.persistence;

import dev.kavrin.paymentrisk.outbox.application.OutboxRelayEventClaimer;
import dev.kavrin.paymentrisk.outbox.application.OutboxRelayQuery;
import dev.kavrin.paymentrisk.outbox.domain.OutboxEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
@RequiredArgsConstructor
public class DatabaseOutboxRelayEventClaimer implements OutboxRelayEventClaimer {

    private final DatabaseClient databaseClient;
    private final OutboxEventPersistenceMapper mapper;

    @Override
    public Flux<OutboxEvent> claimRelayCandidates(OutboxRelayQuery query, String relayInstanceId) {
        var sql = """
                WITH candidates AS (
                    SELECT event_id
                    FROM outbox_events
                    WHERE status = 'PENDING'
                       OR (status = 'FAILED' AND next_retry_at <= :now)
                    ORDER BY created_at ASC
                    LIMIT :batchSize
                    FOR UPDATE SKIP LOCKED
                )
                UPDATE outbox_events events
                SET status = 'PUBLISHING',
                    locked_at = :now,
                    relay_instance_id = :relayInstanceId
                FROM candidates
                WHERE events.event_id = candidates.event_id
                RETURNING events.*
                """;

        return databaseClient.sql(sql)
                .bind("now", query.now())
                .bind("batchSize", query.batchSize())
                .bind("relayInstanceId", relayInstanceId)
                .map((row, metadata) -> mapper.toDomain(row))
                .all();
    }
}
