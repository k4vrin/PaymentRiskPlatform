package dev.kavrin.paymentrisk.outbox.domain;

import java.time.Instant;

public record OutboxEvent(
        String eventId,
        String aggregateType,
        String aggregateId,
        String eventType,
        String schemaVersion,
        String producer,
        String correlationId,
        String payloadJson,
        String status,
        int retryCount,
        Instant nextRetryAt,
        String lastError,
        Instant occurredAt,
        Instant createdAt,
        Instant publishedAt,
        Instant lockedAt,
        String relayInstanceId
) {
}
