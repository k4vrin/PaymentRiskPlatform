package dev.kavrin.paymentrisk.ops.application.outbox;

import java.time.Instant;
import java.util.Optional;

public record OpsOutboxInspectionItem(
        String eventId,
        String aggregateId,
        String aggregateType,
        String eventType,
        String schemaVersion,
        String status,
        int retryCount,
        Optional<String> lastError,
        Optional<Instant> nextRetryAt,
        Instant createdAt,
        Instant occurredAt,
        Optional<Instant> publishedAt,
        Optional<String> correlationId,
        Optional<String> payloadPreview
) {
    private static final int MAX_PAYLOAD_PREVIEW_LENGTH = 500;

    public OpsOutboxInspectionItem {
        lastError = normalize(lastError);
        nextRetryAt = normalize(nextRetryAt);
        publishedAt = normalize(publishedAt);
        correlationId = normalize(correlationId);
        payloadPreview = normalizePayloadPreview(payloadPreview);
    }

    private static <T> Optional<T> normalize(Optional<T> value) {
        return value == null ? Optional.empty() : value;
    }

    private static Optional<String> normalizePayloadPreview(Optional<String> value) {
        var normalized = normalize(value);

        return normalized.map(payload -> {
            if (payload.length() <= MAX_PAYLOAD_PREVIEW_LENGTH) {
                return payload;
            }

            return payload.substring(0, MAX_PAYLOAD_PREVIEW_LENGTH) + "...";
        });
    }
}
