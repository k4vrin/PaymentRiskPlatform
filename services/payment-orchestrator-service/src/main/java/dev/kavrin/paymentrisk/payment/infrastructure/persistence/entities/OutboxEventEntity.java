package dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("outbox_events")
public class OutboxEventEntity {

    @Id
    @Column("event_id")
    private String eventId;

    @Column("aggregate_type")
    private String aggregateType;

    @Column("aggregate_id")
    private String aggregateId;

    @Column("event_type")
    private String eventType;

    @Column("schema_version")
    private String schemaVersion;

    @Column("producer")
    private String producer;

    @Column("correlation_id")
    private String correlationId;

    @Column("payload_json")
    private String payloadJson;

    @Column("status")
    private String status;

    @Column("retry_count")
    private int retryCount;

    @Column("next_retry_at")
    private Instant nextRetryAt;

    @Column("last_error")
    private String lastError;

    @Column("occurred_at")
    private Instant occurredAt;

    @Column("created_at")
    private Instant createdAt;

    @Column("published_at")
    private Instant publishedAt;

    @Column("locked_at")
    private Instant lockedAt;
}
