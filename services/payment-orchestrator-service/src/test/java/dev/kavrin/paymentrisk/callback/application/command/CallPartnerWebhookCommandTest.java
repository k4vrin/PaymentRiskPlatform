package dev.kavrin.paymentrisk.callback.application.command;

import dev.kavrin.paymentrisk.callback.domain.CallbackType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CallPartnerWebhookCommandTest {

    @Test
    void shouldTrimRequiredFields() {
        var command = new CallPartnerWebhookCommand(
                " pay_123 ",
                " mer_123 ",
                " https://partner.example/callback ",
                CallbackType.PAYMENT_AUTHORIZED,
                0,
                " corr_123 "
        );

        assertThat(command.paymentId()).isEqualTo("pay_123");
        assertThat(command.merchantId()).isEqualTo("mer_123");
        assertThat(command.targetUrl()).isEqualTo("https://partner.example/callback");
        assertThat(command.correlationId()).isEqualTo("corr_123");
    }

    @Test
    void shouldRejectInvalidCommand() {
        assertThatThrownBy(() -> new CallPartnerWebhookCommand(
                "",
                "mer_123",
                "https://partner.example/callback",
                CallbackType.PAYMENT_AUTHORIZED,
                0,
                "corr_123"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("paymentId is required");

        assertThatThrownBy(() -> new CallPartnerWebhookCommand(
                "pay_123",
                "mer_123",
                "https://partner.example/callback",
                null,
                0,
                "corr_123"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("callbackType is required");

        assertThatThrownBy(() -> new CallPartnerWebhookCommand(
                "pay_123",
                "mer_123",
                "https://partner.example/callback",
                CallbackType.PAYMENT_AUTHORIZED,
                -1,
                "corr_123"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("attempt must not be negative");
    }
}
