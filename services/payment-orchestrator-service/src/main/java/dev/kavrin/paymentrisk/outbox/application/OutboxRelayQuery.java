package dev.kavrin.paymentrisk.outbox.application;

import lombok.Builder;

import java.time.Instant;

@Builder
public record OutboxRelayQuery(
        Instant now,
        int batchSize,
        boolean skipLocked
) {

    public static final int DEFAULT_BATCH_SIZE = 50;
    public static final int MAX_BATCH_SIZE = 500;

    public OutboxRelayQuery {
        if (now == null) {
            now = Instant.now();
        }

        if (batchSize <= 0) {
            batchSize = DEFAULT_BATCH_SIZE;
        }

        if (batchSize > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("batchSize must not be greater than " + MAX_BATCH_SIZE);
        }
    }

    public static OutboxRelayQuery defaultQuery() {
        return OutboxRelayQuery.builder()
                .now(Instant.now())
                .batchSize(DEFAULT_BATCH_SIZE)
                .skipLocked(true)
                .build();
    }
}
