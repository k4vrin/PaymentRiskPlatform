package dev.kavrin.paymentrisk.ops.api.consumerlag.dto;

import dev.kavrin.paymentrisk.ops.application.consumerlag.ConsumerLagStatus;

import java.time.Instant;

public record ConsumerLagItemResponse(
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
