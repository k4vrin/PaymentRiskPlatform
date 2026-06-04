package dev.kavrin.paymentrisk.ops.api.deadletter;

import static org.assertj.core.api.Assertions.assertThat;

import dev.kavrin.paymentrisk.ops.api.OpsApiPaths;
import dev.kavrin.paymentrisk.ops.application.deadletter.OpsDeadLetterInspectionRequest;
import dev.kavrin.paymentrisk.ops.application.deadletter.OpsDeadLetterInspectionService;
import dev.kavrin.paymentrisk.ops.application.deadletter.OpsDeadLetterItem;
import dev.kavrin.paymentrisk.ops.application.deadletter.OpsDeadLetterResult;
import dev.kavrin.paymentrisk.ops.application.deadletter.ReplayEligibility;
import dev.kavrin.paymentrisk.shared.api.correlation.CorrelationIdWebFilter;
import dev.kavrin.paymentrisk.shared.api.correlation.CorrelationIds;
import dev.kavrin.paymentrisk.shared.api.error.GlobalApiExceptionHandler;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
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

@WebFluxTest(
        controllers = OpsDeadLetterController.class,
        excludeAutoConfiguration = ReactiveWebSecurityAutoConfiguration.class
)
@Import({
        CorrelationIdWebFilter.class,
        GlobalApiExceptionHandler.class,
        OpsDeadLetterInspectionResponseMapper.class,
        OpsDeadLetterControllerTest.TestOpsDeadLetterInspectionConfiguration.class
})
class OpsDeadLetterControllerTest {

    private static final String PATH = OpsApiPaths.OPS_API_V1 + "/dead-letters";
    private static final Instant FAILED_AT = Instant.parse("2026-06-04T10:00:00Z");

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private CapturingOpsDeadLetterInspectionService deadLetterInspectionService;

    @BeforeEach
    void resetService() {
        deadLetterInspectionService.reset();
    }

    @Test
    void inspectDeadLettersMapsQueryParamsAndReturnsItems() {
        deadLetterInspectionService.nextResult = new OpsDeadLetterResult(
                List.of(deadLetterItem()),
                Optional.of("next_token_123")
        );

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(PATH)
                        .queryParam("sourceSystem", "KAFKA")
                        .queryParam("status", "RECORDED")
                        .queryParam("destinationName", "payment.authorization.completed")
                        .queryParam("eventId", "evt_001")
                        .queryParam("messageId", "msg_001")
                        .queryParam("failedFrom", "2026-06-04T09:00:00Z")
                        .queryParam("failedTo", "2026-06-04T11:00:00Z")
                        .queryParam("size", "25")
                        .queryParam("pageToken", "opaque_token")
                        .build())
                .header(CorrelationIds.HEADER_NAME, "corr-dead-letter")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(CorrelationIds.HEADER_NAME, "corr-dead-letter")
                .expectBody()
                .jsonPath("$.items[0].deadLetterId").isEqualTo("dlq_001")
                .jsonPath("$.items[0].sourceSystem").isEqualTo("KAFKA")
                .jsonPath("$.items[0].destinationName").isEqualTo("payment.authorization.completed")
                .jsonPath("$.items[0].status").isEqualTo("RECORDED")
                .jsonPath("$.items[0].partition").isEqualTo(3)
                .jsonPath("$.items[0].offset").isEqualTo(42)
                .jsonPath("$.items[0].eventId").isEqualTo("evt_001")
                .jsonPath("$.items[0].messageId").isEqualTo("msg_001")
                .jsonPath("$.items[0].failureReason").isEqualTo("deserialization failed")
                .jsonPath("$.items[0].replayEligibility").isEqualTo("ELIGIBLE")
                .jsonPath("$.items[0].correlationId").isEqualTo("corr_001")
                .jsonPath("$.items[0].payloadPreview").doesNotExist()
                .jsonPath("$.page.size").isEqualTo(1)
                .jsonPath("$.page.nextPageToken").isEqualTo("next_token_123")
                .jsonPath("$.page.hasNext").isEqualTo(true);

        OpsDeadLetterInspectionRequest request = deadLetterInspectionService.lastRequest.get();
        assertThat(request.sourceSystem()).contains("KAFKA");
        assertThat(request.status()).contains("RECORDED");
        assertThat(request.destinationName()).contains("payment.authorization.completed");
        assertThat(request.eventId()).contains("evt_001");
        assertThat(request.messageId()).contains("msg_001");
        assertThat(request.failedFrom()).contains(Instant.parse("2026-06-04T09:00:00Z"));
        assertThat(request.failedTo()).contains(Instant.parse("2026-06-04T11:00:00Z"));
        assertThat(request.pageSize()).isEqualTo(25);
        assertThat(request.pageToken()).contains("opaque_token");
    }

    @Test
    void inspectDeadLettersReturnsEmptyPage() {
        deadLetterInspectionService.nextResult = new OpsDeadLetterResult(List.of(), Optional.empty());

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
    void inspectDeadLettersReturnsValidationErrorForInvalidFailedRange() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(PATH)
                        .queryParam("failedFrom", "2026-06-04T11:00:00Z")
                        .queryParam("failedTo", "2026-06-04T09:00:00Z")
                        .build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_REQUEST")
                .jsonPath("$.message").isEqualTo("failedFrom must be before or equal to failedTo");
    }

    @Test
    void inspectDeadLettersReturnsValidationErrorForOversizedPage() {
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

    private static OpsDeadLetterItem deadLetterItem() {
        return new OpsDeadLetterItem(
                "dlq_001",
                "KAFKA",
                "payment.authorization.completed",
                "RECORDED",
                Optional.of(3),
                Optional.of(42L),
                Optional.empty(),
                Optional.of("evt_001"),
                Optional.of("msg_001"),
                "deserialization failed",
                FAILED_AT,
                ReplayEligibility.ELIGIBLE,
                Optional.of("corr_001")
        );
    }

    @TestConfiguration
    static class TestOpsDeadLetterInspectionConfiguration {

        @Bean
        CapturingOpsDeadLetterInspectionService deadLetterInspectionService() {
            return new CapturingOpsDeadLetterInspectionService();
        }
    }

    static class CapturingOpsDeadLetterInspectionService implements OpsDeadLetterInspectionService {
        final AtomicReference<OpsDeadLetterInspectionRequest> lastRequest = new AtomicReference<>();
        OpsDeadLetterResult nextResult = new OpsDeadLetterResult(List.of(), Optional.empty());

        @Override
        public Mono<OpsDeadLetterResult> inspect(OpsDeadLetterInspectionRequest request) {
            lastRequest.set(request);
            return Mono.just(nextResult);
        }

        void reset() {
            lastRequest.set(null);
            nextResult = new OpsDeadLetterResult(List.of(), Optional.empty());
        }
    }
}
