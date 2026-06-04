package dev.kavrin.paymentrisk.ops.api.outbox.dto;

import java.time.Instant;

public record OpsOutboxInspectionItemResponse(
        String eventId,
        String aggregateId,
        String aggregateType,
        String eventType,
        String schemaVersion,
        String status,
        int retryCount,
        String lastError,
        Instant nextRetryAt,
        Instant createdAt,
        Instant occurredAt,
        Instant publishedAt,
        String correlationId,
        String payloadPreview
) {
}
