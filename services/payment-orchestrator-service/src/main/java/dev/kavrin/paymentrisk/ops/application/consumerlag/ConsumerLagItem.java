package dev.kavrin.paymentrisk.ops.application.consumerlag;

import java.time.Instant;

public record ConsumerLagItem(
        String consumerGroup,
        String topic,
        int partition,
        long currentOffset,
        long endOffset,
        long lag,
        Instant lastObservedAt,
        ConsumerLagStatus status
) {
}
