package dev.kavrin.paymentrisk.ops.infrastructure.persistence;

import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Durable operational counter built from consumed platform/payment events.
 *
 * <p>This projection supports ops dashboards and APIs without requiring them
 * to scan raw Kafka topics or audit history tables.</p>
 */
@Builder(toBuilder = true)
@Table("ops_event_metrics")
public record OpsEventMetricEntity(
        @Id Long id,
        String metricKey,
        Long metricValue,
        String lastEventId,
        String lastEventType,
        String lastCorrelationId,
        Instant lastObservedAt,
        Instant createdAt,
        Instant updatedAt
) {
}