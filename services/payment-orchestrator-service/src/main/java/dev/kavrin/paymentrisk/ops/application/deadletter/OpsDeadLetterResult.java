package dev.kavrin.paymentrisk.ops.application.deadletter;

import java.util.List;
import java.util.Optional;

public record OpsDeadLetterResult(
        List<OpsDeadLetterItem> items,
        Optional<String> nextPageToken
) {
    public OpsDeadLetterResult {
        items = List.copyOf(items);
        nextPageToken = nextPageToken == null
                ? Optional.empty()
                : nextPageToken;
    }
}