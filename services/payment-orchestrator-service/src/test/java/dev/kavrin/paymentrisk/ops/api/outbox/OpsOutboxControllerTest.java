package dev.kavrin.paymentrisk.ops.api.outbox;

import dev.kavrin.paymentrisk.ops.api.OpsApiPaths;
import dev.kavrin.paymentrisk.ops.application.outbox.OpsOutboxInspectionItem;
import dev.kavrin.paymentrisk.ops.application.outbox.OpsOutboxInspectionRequest;
import dev.kavrin.paymentrisk.ops.application.outbox.OpsOutboxInspectionResult;
import dev.kavrin.paymentrisk.ops.application.outbox.OpsOutboxInspectionService;
import dev.kavrin.paymentrisk.shared.api.correlation.CorrelationIdWebFilter;
import dev.kavrin.paymentrisk.shared.api.correlation.CorrelationIds;
import dev.kavrin.paymentrisk.shared.api.error.GlobalApiExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.web.reactive.ReactiveWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@WebFluxTest(
        controllers = OpsOutboxController.class,
        excludeAutoConfiguration = ReactiveWebSecurityAutoConfiguration.class
)
@Import({
        CorrelationIdWebFilter.class,
        GlobalApiExceptionHandler.class,
        OpsOutboxInspectionResponseMapper.class,
        OpsOutboxControllerTest.TestOpsOutboxInspectionConfiguration.class
})
class OpsOutboxControllerTest {

    private static final String PATH = OpsApiPaths.OPS_API_V1 + "/outbox";
    private static final Instant NOW = Instant.parse("2026-06-04T10:00:00Z");

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private CapturingOpsOutboxInspectionService outboxInspectionService;

    @BeforeEach
    void resetService() {
        outboxInspectionService.reset();
    }

    @Test
    void inspectOutboxMapsQueryParamsAndReturnsItems() {
        outboxInspectionService.nextResult = new OpsOutboxInspectionResult(
                List.of(outboxItem()),
                Optional.of("next_token_123")
        );

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(PATH)
                        .queryParam("status", "FAILED")
                        .queryParam("eventType", "PaymentReversed")
                        .queryParam("aggregateId", "pay_test_123")
                        .queryParam("createdFrom", "2026-06-04T09:00:00Z")
                        .queryParam("createdTo", "2026-06-04T11:00:00Z")
                        .queryParam("size", "25")
                        .queryParam("pageToken", "opaque_token")
                        .build())
                .header(CorrelationIds.HEADER_NAME, "corr-outbox")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(CorrelationIds.HEADER_NAME, "corr-outbox")
                .expectBody()
                .jsonPath("$.items[0].eventId").isEqualTo("evt_test_123")
                .jsonPath("$.items[0].aggregateId").isEqualTo("pay_test_123")
                .jsonPath("$.items[0].schemaVersion").isEqualTo("v1")
                .jsonPath("$.items[0].status").isEqualTo("FAILED")
                .jsonPath("$.items[0].retryCount").isEqualTo(2)
                .jsonPath("$.items[0].lastError").isEqualTo("Kafka unavailable")
                .jsonPath("$.items[0].payloadPreview").isEqualTo("{\"paymentId\":\"pay_test_123\"}")
                .jsonPath("$.page.size").isEqualTo(1)
                .jsonPath("$.page.nextPageToken").isEqualTo("next_token_123")
                .jsonPath("$.page.hasNext").isEqualTo(true);

        OpsOutboxInspectionRequest request = outboxInspectionService.lastRequest.get();
        assertThat(request.status()).contains("FAILED");
        assertThat(request.eventType()).contains("PaymentReversed");
        assertThat(request.aggregateId()).contains("pay_test_123");
        assertThat(request.createdFrom()).contains(Instant.parse("2026-06-04T09:00:00Z"));
        assertThat(request.createdTo()).contains(Instant.parse("2026-06-04T11:00:00Z"));
        assertThat(request.pageSize()).isEqualTo(25);
        assertThat(request.pageToken()).contains("opaque_token");
    }

    @Test
    void inspectOutboxReturnsEmptyPage() {
        outboxInspectionService.nextResult = new OpsOutboxInspectionResult(List.of(), Optional.empty());

        webTestClient.get()
                .uri(PATH)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.items.length()").isEqualTo(0)
                .jsonPath("$.page.size").isEqualTo(0)
                .jsonPath("$.page.hasNext").isEqualTo(false)
                .jsonPath("$.page.nextPageToken").doesNotExist();
    }

    @Test
    void inspectOutboxReturnsValidationErrorForInvalidDateRange() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(PATH)
                        .queryParam("createdFrom", "2026-06-04T11:00:00Z")
                        .queryParam("createdTo", "2026-06-04T09:00:00Z")
                        .build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_REQUEST")
                .jsonPath("$.message").isEqualTo("createdFrom must be before or equal to createdTo");
    }

    @Test
    void inspectOutboxReturnsValidationErrorForOversizedPage() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(PATH)
                        .queryParam("size", "101")
                        .build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_REQUEST")
                .jsonPath("$.message").isEqualTo("pageSize must be <= 100");
    }

    private static OpsOutboxInspectionItem outboxItem() {
        return new OpsOutboxInspectionItem(
                "evt_test_123",
                "pay_test_123",
                "PAYMENT",
                "PaymentReversed",
                "v1",
                "FAILED",
                2,
                Optional.of("Kafka unavailable"),
                Optional.of(NOW.plusSeconds(300)),
                NOW,
                NOW.minusSeconds(1),
                Optional.empty(),
                Optional.of("corr_123"),
                Optional.of("{\"paymentId\":\"pay_test_123\"}")
        );
    }

    @TestConfiguration
    static class TestOpsOutboxInspectionConfiguration {

        @Bean
        CapturingOpsOutboxInspectionService outboxInspectionService() {
            return new CapturingOpsOutboxInspectionService();
        }
    }

    static class CapturingOpsOutboxInspectionService implements OpsOutboxInspectionService {
        final AtomicReference<OpsOutboxInspectionRequest> lastRequest = new AtomicReference<>();
        OpsOutboxInspectionResult nextResult = new OpsOutboxInspectionResult(List.of(), Optional.empty());

        @Override
        public Mono<OpsOutboxInspectionResult> inspect(OpsOutboxInspectionRequest request) {
            lastRequest.set(request);
            return Mono.just(nextResult);
        }

        void reset() {
            lastRequest.set(null);
            nextResult = new OpsOutboxInspectionResult(List.of(), Optional.empty());
        }
    }
}
