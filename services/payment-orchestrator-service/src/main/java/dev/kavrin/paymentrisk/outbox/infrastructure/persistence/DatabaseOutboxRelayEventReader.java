package dev.kavrin.paymentrisk.outbox.infrastructure.persistence;

import dev.kavrin.paymentrisk.outbox.application.OutboxRelayEventReader;
import dev.kavrin.paymentrisk.outbox.application.OutboxRelayQuery;
import dev.kavrin.paymentrisk.outbox.domain.OutboxEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
@RequiredArgsConstructor
public class DatabaseOutboxRelayEventReader implements OutboxRelayEventReader {

    private final DatabaseClient databaseClient;
    private final OutboxEventPersistenceMapper mapper;

    @Override
    public Flux<OutboxEvent> findRelayCandidates(OutboxRelayQuery query) {
        var sql = """
                SELECT *
                FROM outbox_events
                WHERE status = 'PENDING'
                   OR (status = 'FAILED' AND next_retry_at <= :now)
                ORDER BY created_at ASC
                LIMIT :batchSize
                """ + lockClause(query);

        return databaseClient.sql(sql)
                .bind("now", query.now())
                .bind("batchSize", query.batchSize())
                .map((row, metadata) -> mapper.toDomain(row))
                .all();
    }

    private String lockClause(OutboxRelayQuery query) {
        // PostgreSQL skips rows locked by another relay transaction. Step 5 will
        // claim rows after this read; this flag keeps the query model ready for
        // concurrent relay workers without changing caller code later.
        return query.skipLocked() ? "FOR UPDATE SKIP LOCKED" : "";
    }
}
