package dev.kavrin.paymentrisk.risk.infrastructure.grpc;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "payment.risk.grpc")
public record RiskGrpcProperties(
        String host,
        int port,
        Duration timeout
) {
    public RiskGrpcProperties {
        if (host == null || host.isBlank()) {
            host = "localhost";
        }

        if (port <= 0) {
            port = 9090;
        }

        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            timeout = Duration.ofMillis(500);
        }
    }
}