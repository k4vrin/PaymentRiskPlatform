package dev.kavrin.paymentrisk.security.ratelimit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Configuration for request rate limiting.
 *
 * <p>we keep the algorithm intentionally simple:
 * a fixed-window counter stored in Redis. This is enough to demonstrate
 * overload protection without introducing distributed token-bucket complexity.
 */
@Validated
@ConfigurationProperties(prefix = "payment-risk.security.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        @Positive
        long merchantLimit,
        @NotNull
        Duration window,
        @NotBlank
        String pathPrefix
) {
    public RateLimitProperties {
        if (window != null && (window.isZero() || window.isNegative())) {
            throw new IllegalArgumentException("window must be positive");
        }
    }
}
