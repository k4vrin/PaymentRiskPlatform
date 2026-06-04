package dev.kavrin.paymentrisk.ops.application.replay;

public record ReplayTarget(
        String targetId,
        boolean replayable,
        String status
) {
}
