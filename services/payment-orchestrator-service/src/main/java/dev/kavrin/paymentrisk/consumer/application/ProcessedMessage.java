package dev.kavrin.paymentrisk.consumer.application;

import java.time.Instant;

public record ProcessedMessage(
        String processedMessageId,
        String consumerName,
        String topic,
        int partition,
        long offset,
        String eventId,
        Instant processedAt
) {
}
