package dev.kavrin.paymentrisk.shared.api.correlation;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class CorrelationIdWebFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String inboundCorrelationId = exchange.getRequest().getHeaders().getFirst(CorrelationIds.HEADER_NAME);
        String correlationId = inboundCorrelationId == null || inboundCorrelationId.isBlank()
                ? UUID.randomUUID().toString()
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
