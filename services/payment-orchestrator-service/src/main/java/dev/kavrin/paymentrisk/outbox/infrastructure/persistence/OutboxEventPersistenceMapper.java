package dev.kavrin.paymentrisk.outbox.infrastructure.persistence;

import dev.kavrin.paymentrisk.outbox.domain.OutboxEvent;
import io.r2dbc.spi.Row;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
class OutboxEventPersistenceMapper {

    OutboxEvent toDomain(Row row) {
        return new OutboxEvent(
                row.get("event_id", String.class),
                row.get("aggregate_type", String.class),
                row.get("aggregate_id", String.class),
                row.get("event_type", String.class),
                row.get("schema_version", String.class),
                row.get("producer", String.class),
                row.get("correlation_id", String.class),
                row.get("payload_json", String.class),
                row.get("status", String.class),
                requireRetryCount(row),
                row.get("next_retry_at", Instant.class),
                row.get("last_error", String.class),
                row.get("occurred_at", Instant.class),
                row.get("created_at", Instant.class),
                row.get("published_at", Instant.class),
                row.get("locked_at", Instant.class),
                row.get("relay_instance_id", String.class)
        );
    }

    private int requireRetryCount(Row row) {
        Integer retryCount = row.get("retry_count", Integer.class);
        if (retryCount == null) {
            throw new IllegalStateException("retry_count is required");
        }
        return retryCount;
    }
}
