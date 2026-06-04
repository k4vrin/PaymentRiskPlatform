package dev.kavrin.paymentrisk.ops.api.deadletter.dto;

import java.time.Instant;

public record OpsDeadLetterItemResponse(
        String deadLetterId,
        String sourceSystem,
        String destinationName,
        String status,
        Integer partition,
        Long offset,
        String deliveryTag,
        String eventId,
        String messageId,
        String failureReason,
        Instant failedAt,
        String replayEligibility,
        String correlationId
) {
}
