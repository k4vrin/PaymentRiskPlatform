package dev.kavrin.paymentrisk.callback.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;

import static org.assertj.core.api.Assertions.assertThat;

class CallbackRabbitConfigurationTest {

    private final CallbackMessagingProperties properties = properties();
    private final CallbackRabbitConfiguration configuration = new CallbackRabbitConfiguration(properties);

    @Test
    void shouldDeclareCallbackExchangeQueueDlqAndBindings() {
        DirectExchange exchange = configuration.callbackExchange();
        Queue queue = configuration.callbackQueue();
        Queue dlq = configuration.callbackDeadLetterQueue();
        Binding binding = configuration.callbackBinding(queue, exchange);
        Binding dlqBinding = configuration.callbackDeadLetterBinding(dlq, exchange);

        assertThat(exchange.getName()).isEqualTo("partner.callback.exchange");
        assertThat(queue.getName()).isEqualTo("partner.callback.commands");
        assertThat(queue.getArguments())
                .containsEntry("x-dead-letter-exchange", "partner.callback.exchange")
                .containsEntry("x-dead-letter-routing-key", "partner.callback.commands.dlq");
        assertThat(dlq.getName()).isEqualTo("partner.callback.commands.dlq");
        assertThat(binding.getRoutingKey()).isEqualTo("partner.callback.command");
        assertThat(dlqBinding.getRoutingKey()).isEqualTo("partner.callback.commands.dlq");
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
