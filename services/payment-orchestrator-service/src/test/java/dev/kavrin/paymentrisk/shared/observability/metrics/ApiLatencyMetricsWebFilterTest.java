package dev.kavrin.paymentrisk.shared.observability.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.HandlerMapping;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class ApiLatencyMetricsWebFilterTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final ApiLatencyMetricsWebFilter filter = new ApiLatencyMetricsWebFilter(meterRegistry);

    @Test
    void recordsRequestDurationByRouteAndStatus() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/payments/pay_123")
        );
        exchange.getAttributes().put(
                HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,
                "/api/v1/payments/{paymentId}"
        );

        StepVerifier.create(filter.filter(exchange, next -> {
            next.getResponse().setStatusCode(HttpStatus.OK);
            return Mono.empty();
        })).verifyComplete();

        var timer = meterRegistry.timer(
                "paymentrisk.api.request.duration",
                "method", "GET",
                "route", "/api/v1/payments/{paymentId}",
                "status", "200",
                "status_class", "2xx"
        );

        assertThat(timer.count()).isEqualTo(1);
        assertThat(meterRegistry.find("paymentrisk.api.request.duration")
                .tag("route", "/api/v1/payments/pay_123")
                .timer()).isNull();
    }

    @Test
    void recordsUnknownRouteAndErrorStatusClass() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/payments/authorize")
        );

        StepVerifier.create(filter.filter(exchange, next -> {
            next.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return Mono.empty();
        })).verifyComplete();

        var timer = meterRegistry.timer(
                "paymentrisk.api.request.duration",
                "method", "POST",
                "route", "UNKNOWN",
                "status", "500",
                "status_class", "5xx"
        );

        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    void treatsCompletedResponseWithoutExplicitStatusAsOk() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/health")
        );

        StepVerifier.create(filter.filter(exchange, next -> Mono.empty()))
                .verifyComplete();

        var timer = meterRegistry.timer(
                "paymentrisk.api.request.duration",
                "method", "GET",
                "route", "UNKNOWN",
                "status", "200",
                "status_class", "2xx"
        );

        assertThat(timer.count()).isEqualTo(1);
    }
}
