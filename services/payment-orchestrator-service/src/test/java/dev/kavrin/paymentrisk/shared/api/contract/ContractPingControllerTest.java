package dev.kavrin.paymentrisk.shared.api.contract;

import dev.kavrin.paymentrisk.shared.api.correlation.CorrelationIdWebFilter;
import dev.kavrin.paymentrisk.shared.api.correlation.CorrelationIds;
import dev.kavrin.paymentrisk.shared.api.version.ApiPaths;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(controllers = ContractPingController.class)
@Import(CorrelationIdWebFilter.class)
@WithMockUser(roles = "SERVICE")
public class ContractPingControllerTest {

    private static final String PATH = ApiPaths.API_V1 + "/contract/ping";

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void contractPingReturnsOk() {
        webTestClient.get()
                .uri(PATH)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.serviceName").isEqualTo("payment-orchestrator-service")
                .jsonPath("$.apiVersion").isEqualTo("v1")
                .jsonPath("$.correlationId").isNotEmpty();
    }

    @Test
    void contractPingIncludesCorrelationIdHeader() {
        webTestClient.get()
                .uri(PATH)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists(CorrelationIds.HEADER_NAME);
    }

    @Test
    void contractPingPreservesInboundCorrelationId() {
        String correlationId = "test-correlation-id-123";
        webTestClient.get()
                .uri(PATH)
                .header(CorrelationIds.HEADER_NAME, correlationId)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(CorrelationIds.HEADER_NAME, correlationId)
                .expectBody()
                .jsonPath("$.correlationId").isEqualTo(correlationId);
    }

    @Test
    void contractPingGeneratesMissingCorrelationId() {
        webTestClient.get()
                .uri(PATH)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().value(CorrelationIds.HEADER_NAME, value -> {
                    if (value == null || value.isBlank()) {
                        throw new AssertionError("Expected generated correlation ID");
                    }
                })
                .expectBody()
                .jsonPath("$.correlationId").isNotEmpty();
    }
}
