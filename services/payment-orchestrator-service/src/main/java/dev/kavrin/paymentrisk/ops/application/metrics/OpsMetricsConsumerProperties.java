package dev.kavrin.paymentrisk.ops.application.metrics;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for the ops metrics Kafka consumer.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "payment-risk.kafka.consumers.ops-metrics")
public class OpsMetricsConsumerProperties {

    private boolean enabled = false;

    @NotBlank
    private String consumerName = "ops-metrics-consumer";

    @NotBlank
    private String expectedSchemaVersion = "v1";
}
