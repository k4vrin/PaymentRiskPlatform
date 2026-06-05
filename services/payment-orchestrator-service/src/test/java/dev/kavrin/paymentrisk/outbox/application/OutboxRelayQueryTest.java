package dev.kavrin.paymentrisk.outbox.application;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxRelayQueryTest {

    @Test
    void shouldUseSafeDefaults() {
        var query = new OutboxRelayQuery(null, 0, true);

        assertThat(query.now()).isNotNull();
        assertThat(query.batchSize()).isEqualTo(OutboxRelayQuery.DEFAULT_BATCH_SIZE);
        assertThat(query.skipLocked()).isTrue();
    }

    @Test
    void shouldRejectTooLargeBatchSize() {
        assertThatThrownBy(() -> new OutboxRelayQuery(
                Instant.parse("2026-06-05T08:00:00Z"),
                OutboxRelayQuery.MAX_BATCH_SIZE + 1,
                true
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("batchSize must not be greater than " + OutboxRelayQuery.MAX_BATCH_SIZE);
    }
}
