package dev.kavrin.paymentrisk.security.application;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for merchant API key secret hashing.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "payment-risk.security.api-key.hashing")
public class ApiKeyHashingProperties {

    @NotBlank
    private String pepper;
}