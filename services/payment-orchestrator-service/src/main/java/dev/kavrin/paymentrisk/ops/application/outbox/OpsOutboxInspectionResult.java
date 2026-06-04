package dev.kavrin.paymentrisk.ops.application.outbox;

import java.util.List;
import java.util.Optional;

public record OpsOutboxInspectionResult(
        List<OpsOutboxInspectionItem> items,
        Optional<String> nextPageToken
) {
    public OpsOutboxInspectionResult {
        items = List.copyOf(items);
        nextPageToken = nextPageToken == null ? Optional.empty() : nextPageToken;
    }
}