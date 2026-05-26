package dev.kavrin.paymentrisk.shared.api.correlation;

import dev.kavrin.paymentrisk.shared.id.PlatformIdGeneratorFactory;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdWebFilterTest {

    private final CorrelationIdWebFilter filter = new CorrelationIdWebFilter(new PlatformIdGeneratorFactory());

    @Test
    void preservesInboundCorrelationId() {
        String correlationId = "corr-existing";
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/contract/ping")
                        .header(CorrelationIds.HEADER_NAME, correlationId)
        );
        AtomicReference<ServerWebExchange> filteredExchange = new AtomicReference<>();

        filter.filter(exchange, next -> {
            filteredExchange.set(next);
            return next.getResponse().setComplete();
        }).block();

        String resolvedCorrelationId = filteredExchange.get().getAttribute(CorrelationIds.ATTRIBUTE_NAME);

        assertThat(resolvedCorrelationId).isEqualTo(correlationId);
        assertThat(filteredExchange.get().getRequest().getHeaders().getFirst(CorrelationIds.HEADER_NAME))
                .isEqualTo(correlationId);
        assertThat(filteredExchange.get().getResponse().getHeaders().getFirst(CorrelationIds.HEADER_NAME))
                .isEqualTo(correlationId);
    }

    @Test
    void generatesMissingCorrelationId() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/contract/ping")
        );
        AtomicReference<ServerWebExchange> filteredExchange = new AtomicReference<>();

        filter.filter(exchange, next -> {
            filteredExchange.set(next);
            return next.getResponse().setComplete();
        }).block();

        String generatedCorrelationId = filteredExchange.get().getAttribute(CorrelationIds.ATTRIBUTE_NAME);

        assertThat(generatedCorrelationId).isNotBlank();
        assertThat(UUID.fromString(generatedCorrelationId)).isNotNull();
        assertThat(filteredExchange.get().getRequest().getHeaders().getFirst(CorrelationIds.HEADER_NAME))
                .isEqualTo(generatedCorrelationId);
        assertThat(filteredExchange.get().getResponse().getHeaders().getFirst(CorrelationIds.HEADER_NAME))
                .isEqualTo(generatedCorrelationId);
    }
}
