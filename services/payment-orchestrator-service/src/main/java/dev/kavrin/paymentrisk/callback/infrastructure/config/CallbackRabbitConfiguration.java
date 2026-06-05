package dev.kavrin.paymentrisk.callback.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ topology for partner callback processing.
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "payment-risk.rabbitmq.callback", name = "enabled", havingValue = "true")
public class CallbackRabbitConfiguration {

    private final CallbackMessagingProperties properties;

    @Bean
    public DirectExchange callbackExchange() {
        return new DirectExchange(properties.getExchange());
    }

    @Bean
    public Queue callbackQueue() {
        return QueueBuilder.durable(properties.getQueue())
                .deadLetterExchange(properties.getExchange())
                .deadLetterRoutingKey(properties.getDeadLetterQueue())
                .build();
    }

    @Bean
    public Queue callbackDeadLetterQueue() {
        return QueueBuilder.durable(properties.getDeadLetterQueue())
                .build();
    }

    @Bean
    public Binding callbackBinding(
            Queue callbackQueue,
            DirectExchange callbackExchange
    ) {
        return BindingBuilder.bind(callbackQueue)
                .to(callbackExchange)
                .with(properties.getRoutingKey());
    }

    @Bean
    public Binding callbackDeadLetterBinding(
            Queue callbackDeadLetterQueue,
            DirectExchange callbackExchange
    ) {
        return BindingBuilder.bind(callbackDeadLetterQueue)
                .to(callbackExchange)
                .with(properties.getDeadLetterQueue());
    }
}
