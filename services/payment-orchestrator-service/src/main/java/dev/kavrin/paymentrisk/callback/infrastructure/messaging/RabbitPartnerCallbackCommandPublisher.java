package dev.kavrin.paymentrisk.callback.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kavrin.paymentrisk.callback.application.PartnerCallbackCommandPublisher;
import dev.kavrin.paymentrisk.callback.application.command.CallPartnerWebhookCommand;
import dev.kavrin.paymentrisk.callback.infrastructure.config.CallbackMessagingProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "payment-risk.rabbitmq.callback", name = "enabled", havingValue = "true")
public class RabbitPartnerCallbackCommandPublisher implements PartnerCallbackCommandPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final CallbackMessagingProperties properties;

    @Override
    public Mono<Void> publish(CallPartnerWebhookCommand command) {
        return Mono.fromCallable(() -> {
            var json = objectMapper.writeValueAsString(command);

            rabbitTemplate.convertAndSend(
                    properties.getExchange(),
                    properties.getRoutingKey(),
                    json
            );

            return true;
        }).then();
    }
}
