package dev.kavrin.paymentrisk.callback.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import dev.kavrin.paymentrisk.callback.infrastructure.config.CallbackMessagingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.api.RabbitListenerErrorHandler;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Handles partner callback listener failures.
 *
 * <p>Retryable failures are republished as a new command. Terminal failures are
 * rejected without requeue so RabbitMQ routes them to the DLQ.</p>
 */
@Slf4j
@Component("partnerCallbackRabbitErrorHandler")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "payment-risk.rabbitmq.callback", name = "enabled", havingValue = "true")
public class PartnerCallbackRabbitErrorHandler implements RabbitListenerErrorHandler {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final CallbackMessagingProperties properties;

    @Override
    public Object handleError(
            Message amqpMessage,
            Channel channel,
            org.springframework.messaging.Message<?> message,
            ListenerExecutionFailedException exception
    ) throws Exception {
        var retryException = findRetryException(exception);

        if (retryException == null) {
            throw new AmqpRejectAndDontRequeueException(
                    "Terminal partner callback failure",
                    exception
            );
        }

        var retryCommand = retryException.retryCommand();
        var retryJson = objectMapper.writeValueAsString(retryCommand);

        rabbitTemplate.convertAndSend(
                properties.getExchange(),
                properties.getRoutingKey(),
                retryJson,
                outgoing -> {
                    outgoing.getMessageProperties()
                            .setContentType(MessageProperties.CONTENT_TYPE_JSON);
                    outgoing.getMessageProperties()
                            .setCorrelationId(amqpMessage.getMessageProperties().getCorrelationId());
                    return outgoing;
                }
        );

        log.info(
                "Republished partner callback retry paymentId={} merchantId={} attempt={}",
                retryCommand.paymentId(),
                retryCommand.merchantId(),
                retryCommand.attempt()
        );

        return null;
    }

    private static CallbackRetryRequestedException findRetryException(Throwable error) {
        var current = error;

        while (current != null) {
            if (current instanceof CallbackRetryRequestedException retryException) {
                return retryException;
            }

            current = current.getCause();
        }

        return null;
    }
}
