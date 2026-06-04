package dev.kavrin.paymentrisk.ops.application;

import java.time.Instant;
import java.util.Optional;

public record OpsDeadLetterItem(
        String deadLetterId,
        String sourceSystem,
        String destinationName,
        String status,
        Optional<Integer> partition,
        Optional<Long> offset,
        Optional<String> deliveryTag,
        Optional<String> eventId,
        Optional<String> messageId,
        String failureReason,
        Instant failedAt,
        ReplayEligibility replayEligibility,
        Optional<String> correlationId
) {

    public OpsDeadLetterItem {
        partition = normalize(partition);
        offset = normalize(offset);
        deliveryTag = normalize(deliveryTag);
        eventId = normalize(eventId);
        messageId = normalize(messageId);
        correlationId = normalize(correlationId);
    }

    private static <T> Optional<T> normalize(Optional<T> value) {
        return value == null
                ? Optional.empty()
                : value;
    }
}
