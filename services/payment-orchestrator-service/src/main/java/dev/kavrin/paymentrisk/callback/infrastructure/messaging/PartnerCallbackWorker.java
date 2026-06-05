package dev.kavrin.paymentrisk.callback.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kavrin.paymentrisk.callback.application.PartnerWebhookClient;
import dev.kavrin.paymentrisk.callback.application.command.CallPartnerWebhookCommand;
import dev.kavrin.paymentrisk.callback.infrastructure.config.CallbackMessagingProperties;
import dev.kavrin.paymentrisk.shared.messaging.MessagingObservability;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "payment-risk.rabbitmq.callback", name = "enabled", havingValue = "true")
public class PartnerCallbackWorker {

    private final ObjectMapper objectMapper;
    private final PartnerWebhookClient webhookClient;
    private final CallbackMessagingProperties properties;
    private final MessagingObservability observability;

    @RabbitListener(
            queues = "${payment-risk.rabbitmq.callback.queue}",
            errorHandler = "partnerCallbackRabbitErrorHandler"
    )
    public void consume(String messageBody) throws Exception {
        var command = objectMapper.readValue(messageBody, CallPartnerWebhookCommand.class);

        try {
            webhookClient.call(command).block();

            log.info(
                    "Partner callback delivered paymentId={} merchantId={} callbackType={}",
                    command.paymentId(),
                    command.merchantId(),
                    command.callbackType()
            );
            observability.recordCallbackSuccess(command.callbackType().name());
        } catch (Exception error) {
            observability.recordCallbackFailure(command.callbackType().name());
            if (command.attempt() + 1 >= properties.getMaxAttempts()) {
                log.warn(
                        "Partner callback exhausted retries paymentId={} merchantId={} attempt={}",
                        command.paymentId(),
                        command.merchantId(),
                        command.attempt(),
                        error
                );

                throw error;
            }

            var retryCommand = new CallPartnerWebhookCommand(
                    command.paymentId(),
                    command.merchantId(),
                    command.targetUrl(),
                    command.callbackType(),
                    command.attempt() + 1,
                    command.correlationId()
            );

            throw new CallbackRetryRequestedException(retryCommand, error);
        }
    }
}
