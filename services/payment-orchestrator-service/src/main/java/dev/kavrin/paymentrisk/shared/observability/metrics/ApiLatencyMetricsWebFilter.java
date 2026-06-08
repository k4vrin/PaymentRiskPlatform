package dev.kavrin.paymentrisk.shared.observability.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import java.util.concurrent.TimeUnit;

/**
 * Records HTTP API latency without using raw request paths as metric labels.
 *
 * <p>Important: never tag metrics with raw payment IDs, merchant IDs, customer IDs,
 * idempotency keys, or correlation IDs. Those are high-cardinality values and can
 * hurt Prometheus/Grafana performance badly.</p>
 */
@Component
@RequiredArgsConstructor
public class ApiLatencyMetricsWebFilter implements WebFilter {

    private static final String UNKNOWN_ROUTE = "UNKNOWN";

    private final MeterRegistry meterRegistry;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        var startNanos = System.nanoTime();

        return chain.filter(exchange)
                .doFinally(signalType -> record(exchange, startNanos, signalType));
    }

    private void record(ServerWebExchange exchange, long startNanos, SignalType signalType) {
        var durationNanos = System.nanoTime() - startNanos;

        Timer.builder("paymentrisk.api.request.duration")
                .description("HTTP API request duration")
                .tag("method", exchange.getRequest().getMethod().name())
                .tag("route", route(exchange))
                .tag("status", status(exchange, signalType))
                .tag("status_class", statusClass(exchange, signalType))
                .register(meterRegistry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
    }

    private String route(ServerWebExchange exchange) {
        var pattern = exchange.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);

        if (pattern == null) {
            return UNKNOWN_ROUTE;
        }

        return pattern.toString();
    }

    private String status(ServerWebExchange exchange, SignalType signalType) {
        var statusCode = exchange.getResponse().getStatusCode();

        if (statusCode == null) {
            return signalType == SignalType.ON_ERROR ? "UNKNOWN" : "200";
        }

        return Integer.toString(statusCode.value());
    }

    private String statusClass(ServerWebExchange exchange, SignalType signalType) {
        var statusCode = exchange.getResponse().getStatusCode();

        if (statusCode == null) {
            return signalType == SignalType.ON_ERROR ? "UNKNOWN" : "2xx";
        }

        return toStatusClass(statusCode);
    }

    private String toStatusClass(HttpStatusCode statusCode) {
        var value = statusCode.value();

        if (value >= 200 && value < 300) {
            return "2xx";
        }

        if (value >= 300 && value < 400) {
            return "3xx";
        }

        if (value >= 400 && value < 500) {
            return "4xx";
        }

        if (value >= 500) {
            return "5xx";
        }

        return "other";
    }
}
