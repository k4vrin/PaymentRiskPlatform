package dev.kavrin.paymentrisk.callback.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kavrin.paymentrisk.callback.application.PartnerWebhookClient;
import dev.kavrin.paymentrisk.callback.application.command.CallPartnerWebhookCommand;
import dev.kavrin.paymentrisk.callback.domain.CallbackType;
import dev.kavrin.paymentrisk.callback.infrastructure.config.CallbackMessagingProperties;
import dev.kavrin.paymentrisk.shared.messaging.MessagingObservability;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PartnerCallbackWorkerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final FakeWebhookClient webhookClient = new FakeWebhookClient();
    private final PartnerCallbackWorker worker = new PartnerCallbackWorker(
            objectMapper,
            webhookClient,
            properties(),
            new MessagingObservability(meterRegistry)
    );

    @Test
    void shouldCallPartnerWebhookOnSuccess() throws Exception {
        worker.consume(json(command(0)));

        assertThat(webhookClient.calls)
                .extracting(CallPartnerWebhookCommand::paymentId)
                .containsExactly("pay_123");
        assertThat(counter("payment_risk_partner_callback_total", "result", "success")).isEqualTo(1.0);
    }

    @Test
    void shouldRequestRetryForTransientFailure() throws Exception {
        webhookClient.error = new IllegalStateException("timeout");

        assertThatThrownBy(() -> worker.consume(json(command(0))))
                .isInstanceOf(CallbackRetryRequestedException.class)
                .satisfies(error -> {
                    var retry = (CallbackRetryRequestedException) error;
                    assertThat(retry.retryCommand().attempt()).isEqualTo(1);
                    assertThat(retry.retryCommand().paymentId()).isEqualTo("pay_123");
                });

        assertThat(counter("payment_risk_partner_callback_total", "result", "failure")).isEqualTo(1.0);
    }

    @Test
    void shouldThrowTerminalFailureWhenMaxAttemptsReached() throws Exception {
        webhookClient.error = new IllegalStateException("timeout");

        assertThatThrownBy(() -> worker.consume(json(command(2))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("timeout");
    }

    private String json(CallPartnerWebhookCommand command) throws Exception {
        return objectMapper.writeValueAsString(command);
    }

    private static CallPartnerWebhookCommand command(int attempt) {
        return new CallPartnerWebhookCommand(
                "pay_123",
                "mer_123",
                "https://partner.example/callback",
                CallbackType.PAYMENT_AUTHORIZED,
                attempt,
                "corr_123"
        );
    }

    private static CallbackMessagingProperties properties() {
        var properties = new CallbackMessagingProperties();
        properties.setExchange("partner.callback.exchange");
        properties.setQueue("partner.callback.commands");
        properties.setDeadLetterQueue("partner.callback.commands.dlq");
        properties.setRoutingKey("partner.callback.command");
        properties.setMaxAttempts(3);
        return properties;
    }

    private double counter(String name, String tagKey, String tagValue) {
        return meterRegistry.find(name)
                .tag(tagKey, tagValue)
                .counter()
                .count();
    }

    private static final class FakeWebhookClient implements PartnerWebhookClient {

        private final List<CallPartnerWebhookCommand> calls = new ArrayList<>();
        private RuntimeException error;

        @Override
        public Mono<Void> call(CallPartnerWebhookCommand command) {
            calls.add(command);

            if (error != null) {
                return Mono.error(error);
            }

            return Mono.empty();
        }
    }
}
