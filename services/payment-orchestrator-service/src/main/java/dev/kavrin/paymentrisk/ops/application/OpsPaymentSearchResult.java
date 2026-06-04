package dev.kavrin.paymentrisk.ops.application;

import java.util.List;
import java.util.Optional;

public record OpsPaymentSearchResult(
        List<OpsPaymentSearchItem> items,
        Optional<String> nextPageToken
) {
    public OpsPaymentSearchResult {
        items = List.copyOf(items);
        nextPageToken = nextPageToken.isEmpty() ? Optional.empty() : nextPageToken;
    }
}
