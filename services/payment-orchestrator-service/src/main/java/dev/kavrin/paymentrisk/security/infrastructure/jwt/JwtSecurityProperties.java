package dev.kavrin.paymentrisk.security.infrastructure.jwt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Configuration for local JWT validation.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "payment-risk.security.jwt")
public class JwtSecurityProperties {

    @NotBlank
    private String issuer;

    @NotBlank
    private String audience;

    @NotBlank
    private String secret;

    @NotEmpty
    private List<String> allowedOrigins = List.of(
            "http://localhost:3000",
            "http://localhost:5173",
            "http://localhost:8080"
    );

    @NotEmpty
    private List<String> allowedMethods = List.of("GET", "POST", "OPTIONS");

    @NotEmpty
    private List<String> allowedHeaders = List.of(
            "Authorization",
            "Content-Type",
            "X-API-Key",
            "X-Correlation-Id",
            "Idempotency-Key",
            "authorization",
            "content-type",
            "x-api-key",
            "x-correlation-id",
            "idempotency-key",
            "Authorization,X-Correlation-Id"
    );
}
