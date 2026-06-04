package dev.kavrin.paymentrisk.ops.api.dto;

import java.util.List;

public record OpsOutboxInspectionResponse(
        List<OpsOutboxInspectionItemResponse> items,
        OpsPageResponse.PageMetadata page
) {
    public OpsOutboxInspectionResponse {
        items = List.copyOf(items);
    }
}
