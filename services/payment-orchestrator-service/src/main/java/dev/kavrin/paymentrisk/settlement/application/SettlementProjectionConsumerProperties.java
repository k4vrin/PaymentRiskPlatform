package dev.kavrin.paymentrisk.settlement.application;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for the settlement projection Kafka consumer.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "payment-risk.kafka.consumers.settlement-projection")
public class SettlementProjectionConsumerProperties {

    private boolean enabled = false;

    @NotBlank
    private String consumerName = "settlement-projection-consumer";

    @NotBlank
    private String expectedSchemaVersion = "1";
}
