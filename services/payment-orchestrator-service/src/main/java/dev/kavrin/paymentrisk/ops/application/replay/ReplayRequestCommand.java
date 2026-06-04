package dev.kavrin.paymentrisk.ops.application.replay;

import dev.kavrin.paymentrisk.ops.domain.ReplaySource;

import java.util.Optional;

public record ReplayRequestCommand(
        ReplaySource source,
        String targetId,
        String requestedBy,
        Optional<String> reason,
        String correlationId
) {
    public ReplayRequestCommand {
        if (source == null) {
            throw new IllegalArgumentException("replay source is required");
        }
        if (targetId == null || targetId.isBlank()) {
            throw new IllegalArgumentException("target id is required");
        }
        if (requestedBy == null || requestedBy.isBlank()) {
            throw new IllegalArgumentException("requested by is required");
        }
        if (correlationId == null || correlationId.isBlank()) {
            throw new IllegalArgumentException("correlation id is required");
        }
        targetId = targetId.trim();
        requestedBy = requestedBy.trim();
        reason = reason == null ? Optional.empty() : reason.map(String::trim).filter(value -> !value.isBlank());
        correlationId = correlationId.trim();
    }
}
