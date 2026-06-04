package dev.kavrin.paymentrisk.ops.api.outbox;

import dev.kavrin.paymentrisk.ops.api.dto.OpsPageResponse;
import dev.kavrin.paymentrisk.ops.api.outbox.dto.OpsOutboxInspectionItemResponse;
import dev.kavrin.paymentrisk.ops.api.outbox.dto.OpsOutboxInspectionResponse;
import dev.kavrin.paymentrisk.ops.application.outbox.OpsOutboxInspectionItem;
import dev.kavrin.paymentrisk.ops.application.outbox.OpsOutboxInspectionResult;
import org.springframework.stereotype.Component;

@Component
public class OpsOutboxInspectionResponseMapper {

    public OpsOutboxInspectionResponse toResponse(OpsOutboxInspectionResult result) {
        var nextPageToken = result.nextPageToken().orElse(null);
        var items = result.items().stream()
                .map(this::toItemResponse)
                .toList();

        return new OpsOutboxInspectionResponse(
                items,
                new OpsPageResponse.PageMetadata(
                        items.size(),
                        nextPageToken,
                        nextPageToken != null
                )
        );
    }

    private OpsOutboxInspectionItemResponse toItemResponse(
            OpsOutboxInspectionItem item
    ) {
        return new OpsOutboxInspectionItemResponse(
                item.eventId(),
                item.aggregateId(),
                item.aggregateType(),
                item.eventType(),
                item.schemaVersion(),
                item.status(),
                item.retryCount(),
                item.lastError().orElse(null),
                item.nextRetryAt().orElse(null),
                item.createdAt(),
                item.occurredAt(),
                item.publishedAt().orElse(null),
                item.correlationId().orElse(null),
                item.payloadPreview().orElse(null)
        );
    }
}
