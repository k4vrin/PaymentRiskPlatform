package dev.kavrin.paymentrisk.ops.application;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class OpsDeadLetterResultTest {

    @Test
    void createsDeadLetterReadModel() {
        var item = new OpsDeadLetterItem(
                "dlq_001",
                "KAFKA",
                "payment.authorization.completed",
                "RECORDED",
                Optional.of(1),
                Optional.of(42L),
                Optional.empty(),
                Optional.of("evt_001"),
                Optional.of("msg_001"),
                "deserialization failed",
                Instant.parse("2026-06-04T10:00:00Z"),
                ReplayEligibility.ELIGIBLE,
                Optional.of("corr_001")
        );

        var result = new OpsDeadLetterResult(List.of(item), Optional.of("next_token"));

        assertThat(result.items()).containsExactly(item);
        assertThat(result.nextPageToken()).contains("next_token");
        assertThat(item.status()).isEqualTo("RECORDED");
    }

    @Test
    void normalizesNullOptionals() {
        var item = new OpsDeadLetterItem(
                "dlq_001",
                "RABBITMQ",
                "partner.callback.commands.dlq",
                "RECORDED",
                null,
                null,
                null,
                null,
                null,
                "callback failed",
                Instant.parse("2026-06-04T10:00:00Z"),
                ReplayEligibility.NOT_ELIGIBLE,
                null
        );

        assertThat(item.partition()).isEmpty();
        assertThat(item.offset()).isEmpty();
        assertThat(item.deliveryTag()).isEmpty();
        assertThat(item.eventId()).isEmpty();
        assertThat(item.messageId()).isEmpty();
        assertThat(item.correlationId()).isEmpty();
    }
}
