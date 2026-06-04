package dev.kavrin.paymentrisk.ops.application;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class OpsOutboxInspectionResultTest {

    @Test
    void createsOutboxInspectionResult() {
        var item = new OpsOutboxInspectionItem(
                "evt_test_123",
                "pay_test_123",
                "Payment",
                "PaymentReversed",
                "v1",
                "PENDING",
                0,
                Optional.empty(),
                Optional.empty(),
                Instant.parse("2026-06-01T10:00:00Z"),
                Instant.parse("2026-06-01T09:59:59Z"),
                Optional.empty(),
                Optional.of("corr_123"),
                Optional.empty()
        );

        var result = new OpsOutboxInspectionResult(
                List.of(item),
                Optional.of("cursor_next_123")
        );

        assertThat(result.items()).containsExactly(item);
        assertThat(result.nextPageToken()).contains("cursor_next_123");
    }

    @Test
    void normalizesNullOptionalsToEmpty() {
        var item = new OpsOutboxInspectionItem(
                "evt_test_123",
                "pay_test_123",
                "Payment",
                "PaymentAuthorized",
                "v1",
                "FAILED",
                3,
                null,
                null,
                Instant.parse("2026-06-01T10:00:00Z"),
                Instant.parse("2026-06-01T09:59:59Z"),
                null,
                null,
                null
        );

        assertThat(item.lastError()).isEmpty();
        assertThat(item.nextRetryAt()).isEmpty();
        assertThat(item.publishedAt()).isEmpty();
        assertThat(item.correlationId()).isEmpty();
        assertThat(item.payloadPreview()).isEmpty();
    }

    @Test
    void truncatesPayloadPreview() {
        var longPayload = "x".repeat(600);

        var item = new OpsOutboxInspectionItem(
                "evt_test_123",
                "pay_test_123",
                "Payment",
                "PaymentAuthorized",
                "v1",
                "FAILED",
                3,
                Optional.of("Kafka unavailable"),
                Optional.of(Instant.parse("2026-06-01T10:05:00Z")),
                Instant.parse("2026-06-01T10:00:00Z"),
                Instant.parse("2026-06-01T09:59:59Z"),
                Optional.empty(),
                Optional.of("corr_123"),
                Optional.of(longPayload)
        );

        assertThat(item.payloadPreview()).isPresent();
        assertThat(item.payloadPreview().orElseThrow()).hasSize(503);
        assertThat(item.payloadPreview().orElseThrow()).endsWith("...");
    }
}
