package dev.kavrin.paymentrisk.ops.api.replay.dto;

import dev.kavrin.paymentrisk.ops.domain.ReplayJobStatus;
import dev.kavrin.paymentrisk.ops.domain.ReplaySource;

import java.time.Instant;

public record ReplayJobResponse(
        String replayJobId,
        String targetId,
        ReplaySource source,
        String requestedBy,
        Instant requestedAt,
        ReplayJobStatus status,
        String reason,
        String failureReason,
        String correlationId
) {
}
