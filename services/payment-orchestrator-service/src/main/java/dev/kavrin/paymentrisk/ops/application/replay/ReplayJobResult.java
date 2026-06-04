package dev.kavrin.paymentrisk.ops.application.replay;

import dev.kavrin.paymentrisk.ops.domain.ReplayJobStatus;
import dev.kavrin.paymentrisk.ops.domain.ReplaySource;

import java.time.Instant;
import java.util.Optional;

public record ReplayJobResult(
        String replayJobId,
        String targetId,
        ReplaySource source,
        String requestedBy,
        Instant requestedAt,
        ReplayJobStatus status,
        Optional<String> reason,
        Optional<String> failureReason,
        String correlationId
) {
    public ReplayJobResult {
        reason = reason == null ? Optional.empty() : reason;
        failureReason = failureReason == null ? Optional.empty() : failureReason;
    }
}
