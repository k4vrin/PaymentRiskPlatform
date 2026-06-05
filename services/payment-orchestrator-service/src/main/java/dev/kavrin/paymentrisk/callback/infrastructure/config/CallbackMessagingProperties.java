package dev.kavrin.paymentrisk.callback.infrastructure.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * RabbitMQ callback messaging configuration.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "payment-risk.rabbitmq.callback")
public class CallbackMessagingProperties {

    private boolean enabled = false;

    @NotBlank
    private String exchange;

    @NotBlank
    private String queue;

    @NotBlank
    private String deadLetterQueue;

    @NotBlank
    private String routingKey;

    @Min(1)
    private int maxAttempts = 3;

    @NotBlank
    private String commandProducerConsumerName = "partner-callback-command-producer";

    @NotBlank
    private String expectedSchemaVersion = "v1";

    @NotBlank
    private String targetUrlTemplate = "http://localhost:8080/partner-callbacks/{merchantId}";
}
