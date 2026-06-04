package dev.kavrin.paymentrisk.ops.api.deadletter;

import dev.kavrin.paymentrisk.ops.api.deadletter.dto.OpsDeadLetterInspectionResponse;
import dev.kavrin.paymentrisk.ops.api.deadletter.dto.OpsDeadLetterItemResponse;
import dev.kavrin.paymentrisk.ops.api.dto.OpsPageResponse;
import dev.kavrin.paymentrisk.ops.application.deadletter.OpsDeadLetterItem;
import dev.kavrin.paymentrisk.ops.application.deadletter.OpsDeadLetterResult;
import org.springframework.stereotype.Component;

@Component
public class OpsDeadLetterInspectionResponseMapper {

    public OpsDeadLetterInspectionResponse toResponse(OpsDeadLetterResult result) {
        var nextPageToken = result.nextPageToken().orElse(null);
        var items = result.items().stream()
                .map(this::toItemResponse)
                .toList();

        return new OpsDeadLetterInspectionResponse(
                items,
                new OpsPageResponse.PageMetadata(
                        items.size(),
                        nextPageToken,
                        nextPageToken != null
                )
        );
    }

    private OpsDeadLetterItemResponse toItemResponse(OpsDeadLetterItem item) {
        return new OpsDeadLetterItemResponse(
                item.deadLetterId(),
                item.sourceSystem(),
                item.destinationName(),
                item.status(),
                item.partition().orElse(null),
                item.offset().orElse(null),
                item.deliveryTag().orElse(null),
                item.eventId().orElse(null),
                item.messageId().orElse(null),
                item.failureReason(),
                item.failedAt(),
                item.replayEligibility().name(),
                item.correlationId().orElse(null)
        );
    }
}
