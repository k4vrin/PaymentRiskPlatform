package dev.kavrin.paymentrisk.audit.infrastructure.persistence;

import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Durable audit/history row produced from payment lifecycle events.
 *
 * <p>This entity stores the original event payload as JSON so operators can
 * inspect the exact business event that created the audit record.</p>
 */
@Builder
@Table("payment_audit_events")
public record PaymentAuditEventEntity(
        @Id Long id,
        String eventId,
        String eventType,
        String paymentId,
        String aggregateType,
        String schemaVersion,
        String correlationId,
        Instant occurredAt,
        String payloadJson,
        Instant createdAt
) {
}