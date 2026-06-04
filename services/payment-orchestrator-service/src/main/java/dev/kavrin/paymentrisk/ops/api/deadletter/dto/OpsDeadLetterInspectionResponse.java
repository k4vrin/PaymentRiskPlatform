package dev.kavrin.paymentrisk.ops.api.deadletter.dto;

import dev.kavrin.paymentrisk.ops.api.dto.OpsPageResponse;
import java.util.List;

public record OpsDeadLetterInspectionResponse(
        List<OpsDeadLetterItemResponse> items,
        OpsPageResponse.PageMetadata page
) {
    public OpsDeadLetterInspectionResponse {
        items = List.copyOf(items);
    }
}
