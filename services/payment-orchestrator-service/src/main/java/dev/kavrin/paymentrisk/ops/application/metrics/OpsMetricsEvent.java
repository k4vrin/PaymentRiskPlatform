package dev.kavrin.paymentrisk.ops.application.metrics;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

/**
 * Normalized event consumed by the ops metrics projection.
 */
public record OpsMetricsEvent(
        String eventId,
        String eventType,
        String aggregateId,
        String aggregateType,
        String schemaVersion,
        String correlationId,
        Instant occurredAt,
        JsonNode payload
) {
}