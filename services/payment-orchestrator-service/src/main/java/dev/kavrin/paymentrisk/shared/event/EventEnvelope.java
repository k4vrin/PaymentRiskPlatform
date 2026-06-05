package dev.kavrin.paymentrisk.shared.event;

import java.time.Instant;
import java.util.Objects;

public record EventEnvelope<T>(
        String eventId,
        String schemaVersion,
        String eventType,
        String aggregateId,
        String aggregateType,
        Instant occurredAt,
        String producer,
        String correlationId,
        T payload
) {

    public EventEnvelope {
        eventId = requireText(eventId, "eventId");
        schemaVersion = requireText(schemaVersion, "schemaVersion");
        eventType = requireText(eventType, "eventType");
        aggregateId = requireText(aggregateId, "aggregateId");
        aggregateType = requireText(aggregateType, "aggregateType");
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt is required");
        producer = requireText(producer, "producer");
        correlationId = requireText(correlationId, "correlationId");
        payload = Objects.requireNonNull(payload, "payload is required");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }

        return value.trim();
    }
}
