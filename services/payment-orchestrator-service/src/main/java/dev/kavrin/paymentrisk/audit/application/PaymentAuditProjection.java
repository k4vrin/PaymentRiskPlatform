package dev.kavrin.paymentrisk.audit.application;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

/**
 * Audit-friendly representation of a payment lifecycle event consumed from Kafka.
 *
 * <p>This model intentionally stores the original payload as JSON so the audit
 * projection can evolve without forcing every event payload to have a separate
 * audit-specific Java model immediately.</p>
 */
public record PaymentAuditProjection(
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