package dev.kavrin.paymentrisk.ops.api;

import dev.kavrin.paymentrisk.ops.api.dto.OpsOutboxInspectionItemResponse;
import dev.kavrin.paymentrisk.ops.api.dto.OpsOutboxInspectionResponse;
import dev.kavrin.paymentrisk.ops.application.OpsOutboxInspectionItem;
import dev.kavrin.paymentrisk.ops.application.OpsOutboxInspectionResult;
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
                new dev.kavrin.paymentrisk.ops.api.dto.OpsPageResponse.PageMetadata(
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
