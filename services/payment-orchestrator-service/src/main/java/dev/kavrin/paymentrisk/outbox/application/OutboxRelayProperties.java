package dev.kavrin.paymentrisk.outbox.application;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "payment-risk.outbox.relay")
public class OutboxRelayProperties {

    private boolean enabled = false;

    @Min(1)
    @Max(500)
    private int batchSize = 50;

    @Min(1000)
    private long fixedDelayMillis = 5000;

    @NotBlank
    private String instanceId = "payment-orchestrator-service";
}
