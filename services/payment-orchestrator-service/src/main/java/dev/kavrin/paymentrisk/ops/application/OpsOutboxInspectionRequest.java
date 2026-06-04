package dev.kavrin.paymentrisk.ops.application;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record OpsOutboxInspectionRequest(
        Optional<String> status,
        Optional<String> eventType,
        Optional<String> aggregateId,
        Optional<Instant> createdFrom,
        Optional<Instant> createdTo,
        int pageSize,
        Optional<String> pageToken
) {
    public static final int DEFAULT_PAGE_SIZE = 50;
    public static final int MAX_PAGE_SIZE = 100;

    public OpsOutboxInspectionRequest {
        status = normalizeText(status);
        eventType = normalizeText(eventType);
        aggregateId = normalizeText(aggregateId);
        createdFrom = normalize(createdFrom);
        createdTo = normalize(createdTo);
        pageToken = normalizeText(pageToken);

        if (pageSize <= 0) {
            pageSize = DEFAULT_PAGE_SIZE;
        }

        if (pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("pageSize must be <= " + MAX_PAGE_SIZE);
        }

        if (createdFrom.isPresent()
                && createdTo.isPresent()
                && createdFrom.get().isAfter(createdTo.get())) {
            throw new IllegalArgumentException("createdFrom must be before or equal to createdTo");
        }
    }

    private static Optional<String> normalizeText(Optional<String> value) {
        return Objects.requireNonNullElse(value, Optional.<String>empty())
                .map(String::trim)
                .filter(text -> !text.isBlank());
    }

    private static <T> Optional<T> normalize(Optional<T> value) {
        return Objects.requireNonNullElse(value, Optional.empty());
    }
}
