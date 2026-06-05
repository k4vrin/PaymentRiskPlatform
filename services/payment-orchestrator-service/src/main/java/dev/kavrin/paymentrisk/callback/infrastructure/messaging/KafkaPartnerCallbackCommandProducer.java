package dev.kavrin.paymentrisk.callback.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kavrin.paymentrisk.callback.application.PartnerCallbackCommandPublisher;
import dev.kavrin.paymentrisk.callback.application.command.CallPartnerWebhookCommand;
import dev.kavrin.paymentrisk.callback.domain.CallbackType;
import dev.kavrin.paymentrisk.callback.infrastructure.config.CallbackMessagingProperties;
import dev.kavrin.paymentrisk.consumer.application.IdempotentConsumerGuard;
import dev.kavrin.paymentrisk.consumer.application.ProcessedMessageCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "payment-risk.rabbitmq.callback", name = "enabled", havingValue = "true")
public class KafkaPartnerCallbackCommandProducer {

    private final ObjectMapper objectMapper;
    private final CallbackMessagingProperties properties;
    private final IdempotentConsumerGuard consumerGuard;
    private final PartnerCallbackCommandPublisher publisher;

    @KafkaListener(
            groupId = "${payment-risk.rabbitmq.callback.command-producer-consumer-name:partner-callback-command-producer}",
            topics = {
                    "#{@kafkaTopicProperties.topics().paymentAuthorizationCompleted()}",
                    "#{@kafkaTopicProperties.topics().paymentReversalCompleted()}"
            }
    )
    public void consume(ConsumerRecord<String, String> record) {
        handle(record).block();
    }

    Mono<Void> handle(ConsumerRecord<String, String> record) {
        return Mono.fromCallable(() -> commandEnvelope(record))
                .flatMap(envelope -> {
                    var processed = new ProcessedMessageCommand(
                            properties.getCommandProducerConsumerName(),
                            record.topic(),
                            record.partition(),
                            record.offset(),
                            envelope.eventId()
                    );

                    return consumerGuard.processOnce(
                            processed,
                            publisher.publish(envelope.command())
                    );
                })
                .doOnNext(published -> {
                    if (published) {
                        log.info(
                                "Published partner callback command topic={} partition={} offset={}",
                                record.topic(),
                                record.partition(),
                                record.offset()
                        );
                    }
                })
                .then();
    }

    private CommandEnvelope commandEnvelope(ConsumerRecord<String, String> record) throws Exception {
        var root = objectMapper.readTree(record.value());
        var eventId = requiredText(root, "eventId");
        var schemaVersion = requiredText(root, "schemaVersion");

        if (!properties.getExpectedSchemaVersion().equals(schemaVersion)) {
            throw new IllegalArgumentException("Unsupported callback command event schemaVersion=" + schemaVersion);
        }

        var payload = requiredNode(root, "payload");
        var merchantId = requiredText(payload, "merchantId");

        var command = new CallPartnerWebhookCommand(
                requiredText(root, "aggregateId"),
                merchantId,
                targetUrl(payload, merchantId),
                callbackType(requiredText(root, "eventType")),
                0,
                requiredText(root, "correlationId")
        );

        return new CommandEnvelope(eventId, command);
    }

    private String targetUrl(JsonNode payload, String merchantId) {
        var explicitTarget = textOrNull(payload, "callbackUrl");

        if (explicitTarget != null) {
            return explicitTarget;
        }

        return properties.getTargetUrlTemplate().replace("{merchantId}", merchantId);
    }

    private static CallbackType callbackType(String eventType) {
        return switch (eventType) {
            case "PaymentAuthorized" -> CallbackType.PAYMENT_AUTHORIZED;
            case "PaymentDeclined" -> CallbackType.PAYMENT_DECLINED;
            case "PaymentReversed" -> CallbackType.PAYMENT_REVERSED;
            default -> throw new IllegalArgumentException("Unsupported callback event type: " + eventType);
        };
    }

    private static String requiredText(JsonNode root, String fieldName) {
        var value = textOrNull(root, fieldName);

        if (value == null) {
            throw new IllegalArgumentException("Missing required event field: " + fieldName);
        }

        return value;
    }

    private static JsonNode requiredNode(JsonNode root, String fieldName) {
        var node = root.get(fieldName);

        if (node == null || node.isNull()) {
            throw new IllegalArgumentException("Missing required event field: " + fieldName);
        }

        return node;
    }

    private static String textOrNull(JsonNode root, String fieldName) {
        var node = root.get(fieldName);

        if (node == null || node.isNull() || node.asText().isBlank()) {
            return null;
        }

        return node.asText();
    }

    private record CommandEnvelope(String eventId, CallPartnerWebhookCommand command) {
    }
}
