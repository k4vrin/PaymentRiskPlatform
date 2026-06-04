package dev.kavrin.paymentrisk.ops.application.deadletter;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record OpsDeadLetterInspectionRequest(
        Optional<String> sourceSystem,
        Optional<String> status,
        Optional<String> destinationName,
        Optional<String> eventId,
        Optional<String> messageId,
        Optional<Instant> failedFrom,
        Optional<Instant> failedTo,
        int pageSize,
        Optional<String> pageToken
) {
    public static final int DEFAULT_PAGE_SIZE = 50;
    public static final int MAX_PAGE_SIZE = 100;

    public OpsDeadLetterInspectionRequest {
        sourceSystem = normalizeText(sourceSystem);
        status = normalizeText(status);
        destinationName = normalizeText(destinationName);
        eventId = normalizeText(eventId);
        messageId = normalizeText(messageId);
        failedFrom = normalize(failedFrom);
        failedTo = normalize(failedTo);
        pageToken = normalizeText(pageToken);

        if (pageSize <= 0) {
            pageSize = DEFAULT_PAGE_SIZE;
        }

        if (pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("pageSize must be <= " + MAX_PAGE_SIZE);
        }

        if (failedFrom.isPresent()
                && failedTo.isPresent()
                && failedFrom.get().isAfter(failedTo.get())) {
            throw new IllegalArgumentException("failedFrom must be before or equal to failedTo");
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
