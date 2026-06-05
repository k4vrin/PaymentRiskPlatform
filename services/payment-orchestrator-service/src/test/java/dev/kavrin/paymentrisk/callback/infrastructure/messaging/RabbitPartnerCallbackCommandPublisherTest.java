package dev.kavrin.paymentrisk.callback.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kavrin.paymentrisk.callback.application.command.CallPartnerWebhookCommand;
import dev.kavrin.paymentrisk.callback.domain.CallbackType;
import dev.kavrin.paymentrisk.callback.infrastructure.config.CallbackMessagingProperties;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RabbitPartnerCallbackCommandPublisherTest {

    @Test
    void shouldPublishCallbackCommandToConfiguredExchangeAndRoutingKey() {
        var rabbitTemplate = mock(RabbitTemplate.class);
        var publisher = new RabbitPartnerCallbackCommandPublisher(
                rabbitTemplate,
                new ObjectMapper(),
                properties()
        );

        StepVerifier.create(publisher.publish(command()))
                .verifyComplete();

        verify(rabbitTemplate).convertAndSend(
                eq("partner.callback.exchange"),
                eq("partner.callback.command"),
                contains("\"paymentId\":\"pay_123\"")
        );
    }

    private static CallPartnerWebhookCommand command() {
        return new CallPartnerWebhookCommand(
                "pay_123",
                "mer_123",
                "https://partner.example/callback",
                CallbackType.PAYMENT_AUTHORIZED,
                0,
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
