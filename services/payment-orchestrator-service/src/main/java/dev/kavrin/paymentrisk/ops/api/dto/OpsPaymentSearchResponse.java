package dev.kavrin.paymentrisk.ops.api.dto;

import java.util.List;

public record OpsPaymentSearchResponse(
        List<OpsPaymentSearchItemResponse> items,
        OpsPageResponse.PageMetadata page
) {
    public OpsPaymentSearchResponse {
        items = List.copyOf(items);
    }
}
