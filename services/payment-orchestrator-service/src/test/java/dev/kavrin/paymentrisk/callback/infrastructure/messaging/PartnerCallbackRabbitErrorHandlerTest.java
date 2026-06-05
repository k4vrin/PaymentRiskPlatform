package dev.kavrin.paymentrisk.callback.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kavrin.paymentrisk.callback.application.command.CallPartnerWebhookCommand;
import dev.kavrin.paymentrisk.callback.domain.CallbackType;
import dev.kavrin.paymentrisk.callback.infrastructure.config.CallbackMessagingProperties;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PartnerCallbackRabbitErrorHandlerTest {

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final PartnerCallbackRabbitErrorHandler errorHandler = new PartnerCallbackRabbitErrorHandler(
            rabbitTemplate,
            new ObjectMapper(),
            properties()
    );

    @Test
    void shouldRepublishRetryCommandAndAckOriginalFailure() throws Exception {
        var retryException = new CallbackRetryRequestedException(command(1), new IllegalStateException("timeout"));

        errorHandler.handleError(
                amqpMessage(),
                null,
                null,
                new ListenerExecutionFailedException("listener failed", retryException)
        );

        verify(rabbitTemplate).convertAndSend(
                eq("partner.callback.exchange"),
                eq("partner.callback.command"),
                contains("\"attempt\":1"),
                any(MessagePostProcessor.class)
        );
    }

    @Test
    void shouldRejectTerminalFailureWithoutRequeue() {
        assertThatThrownBy(() -> errorHandler.handleError(
                amqpMessage(),
                null,
                null,
                new ListenerExecutionFailedException("listener failed", new IllegalStateException("timeout"))
        ))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasMessage("Terminal partner callback failure");
    }

    private static Message amqpMessage() {
        var properties = new MessageProperties();
        properties.setCorrelationId("corr_123");
        return new Message("{}".getBytes(), properties);
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
}
