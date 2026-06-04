package dev.kavrin.paymentrisk.ops.domain;

import java.time.Instant;
import java.util.Optional;

public record ReplayJob(
        ReplayJobId replayJobId,
        String targetId,
        ReplaySource source,
        String requestedBy,
        Instant requestedAt,
        ReplayJobStatus status,
        Optional<String> failureReason
) {
    public ReplayJob {
        if (replayJobId == null) {
            throw new IllegalArgumentException("replay job id is required");
        }
        if (targetId == null || targetId.isBlank()) {
            throw new IllegalArgumentException("target id is required");
        }
        if (source == null) {
            throw new IllegalArgumentException("replay source is required");
        }
        if (requestedBy == null || requestedBy.isBlank()) {
            throw new IllegalArgumentException("requested by is required");
        }
        if (requestedAt == null) {
            throw new IllegalArgumentException("requested at is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("replay job status is required");
        }

        targetId = targetId.trim();
        requestedBy = requestedBy.trim();
        failureReason = failureReason == null ? Optional.empty() : failureReason;
    }

    public static ReplayJob requested(
            ReplayJobId replayJobId,
            String targetId,
            ReplaySource source,
            String requestedBy,
            Instant requestedAt
    ) {
        return new ReplayJob(
                replayJobId,
                targetId,
                source,
                requestedBy,
                requestedAt,
                ReplayJobStatus.REQUESTED,
                Optional.empty()
        );
    }

    public ReplayJob markRunning() {
        return new ReplayJob(
                replayJobId,
                targetId,
                source,
                requestedBy,
                requestedAt,
                ReplayJobStatus.RUNNING,
                Optional.empty()
        );
    }

    public ReplayJob markSucceeded() {
        return new ReplayJob(
                replayJobId,
                targetId,
                source,
                requestedBy,
                requestedAt,
                ReplayJobStatus.SUCCEEDED,
                Optional.empty()
        );
    }

    public ReplayJob markFailed(String reason) {
        return new ReplayJob(
                replayJobId,
                targetId,
                source,
                requestedBy,
                requestedAt,
                ReplayJobStatus.FAILED,
                Optional.ofNullable(reason)
        );
    }

    public ReplayJob markRejected(String reason) {
        return new ReplayJob(
                replayJobId,
                targetId,
                source,
                requestedBy,
                requestedAt,
                ReplayJobStatus.REJECTED,
                Optional.ofNullable(reason)
        );
    }
}