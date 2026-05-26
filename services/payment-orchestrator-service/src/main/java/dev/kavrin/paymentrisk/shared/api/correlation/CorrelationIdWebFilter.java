package dev.kavrin.paymentrisk.shared.api.correlation;

import dev.kavrin.paymentrisk.shared.id.PlatformIdGeneratorFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Objects;

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
