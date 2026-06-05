package dev.kavrin.paymentrisk.consumer.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessedMessageCommandTest {

    @Test
    void shouldTrimTextFields() {
        var command = new ProcessedMessageCommand(
                " payments-audit ",
                " payment.authorization.completed ",
                0,
                42,
                " evt_1 "
        );

        assertThat(command.consumerName()).isEqualTo("payments-audit");
        assertThat(command.topic()).isEqualTo("payment.authorization.completed");
        assertThat(command.eventId()).isEqualTo("evt_1");
    }

    @Test
    void shouldRejectBlankRequiredFields() {
        assertThatThrownBy(() -> new ProcessedMessageCommand(
                " ",
                "payment.authorization.completed",
                0,
                42,
                "evt_1"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("consumerName is required");
    }

    @Test
    void shouldRejectNegativeKafkaPosition() {
        assertThatThrownBy(() -> new ProcessedMessageCommand(
                "payments-audit",
                "payment.authorization.completed",
                -1,
                42,
                "evt_1"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("partition must not be negative");

        assertThatThrownBy(() -> new ProcessedMessageCommand(
                "payments-audit",
                "payment.authorization.completed",
                0,
                -1,
                "evt_1"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("offset must not be negative");
    }
}
