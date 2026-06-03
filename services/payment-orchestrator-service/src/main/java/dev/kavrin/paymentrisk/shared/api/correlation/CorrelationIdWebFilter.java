package dev.kavrin.paymentrisk.shared.api.correlation;

import dev.kavrin.paymentrisk.shared.id.PlatformIdGeneratorFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * WebFlux filter that establishes a correlation ID for every request.
 * <p>
 * Spring automatically registers this filter because it implements
 * {@link org.springframework.web.server.WebFilter} and is discovered as
 * a Spring bean through {@code @Component} scanning.
 * <p>
 * The filter:
 * - Preserves an inbound X-Correlation-Id when provided.
 * - Generates a new correlation ID when missing.
 * - Stores the correlation ID in exchange attributes for downstream access.
 * - Adds the correlation ID to response headers.
 * <p>
 * Controllers and services should read the correlation ID from the
 * exchange attributes instead of re-reading request headers.
 */
@Component
public class CorrelationIdWebFilter implements WebFilter {

    private final PlatformIdGeneratorFactory idGenerator;

    public CorrelationIdWebFilter() {
        this(new PlatformIdGeneratorFactory());
    }

    CorrelationIdWebFilter(PlatformIdGeneratorFactory idGenerator) {
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String inboundCorrelationId = exchange.getRequest().getHeaders().getFirst(CorrelationIds.HEADER_NAME);
        String correlationId = inboundCorrelationId == null || inboundCorrelationId.isBlank()
                ? idGenerator.correlationId()
                : inboundCorrelationId;

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(builder -> builder.header(CorrelationIds.HEADER_NAME, correlationId))
                .build();

        mutatedExchange.getAttributes().put(CorrelationIds.ATTRIBUTE_NAME, correlationId);
        mutatedExchange.getResponse().getHeaders().set(CorrelationIds.HEADER_NAME, correlationId);

        // TODO: Propagate correlation ID through gRPC metadata.
        // TODO: Propagate correlation ID through Kafka headers.
        // TODO: Propagate correlation ID through RabbitMQ headers.
        return chain.filter(mutatedExchange);
    }
}
