package dev.kavrin.paymentrisk.audit.application;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "payment-risk.kafka.consumers.payment-audit")
public class PaymentAuditConsumerProperties {

    private boolean enabled = false;

    @NotBlank
    private String consumerName = "payment-audit-consumer";

    @NotBlank
    private String expectedSchemaVersion = "v1";
}
