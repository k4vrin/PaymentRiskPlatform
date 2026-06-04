package dev.kavrin.paymentrisk.ops.api.consumerlag.dto;

import java.util.List;

public record ConsumerLagResponse(
        List<ConsumerLagItemResponse> items,
        String unavailableReason
) {
    public ConsumerLagResponse {
        items = List.copyOf(items);
    }
}
