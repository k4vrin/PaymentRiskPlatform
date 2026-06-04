package dev.kavrin.paymentrisk.ops.api.payment;

import dev.kavrin.paymentrisk.ops.api.OpsApiPaths;
import dev.kavrin.paymentrisk.ops.application.payment.OpsPaymentSearchItem;
import dev.kavrin.paymentrisk.ops.application.payment.OpsPaymentSearchRequest;
import dev.kavrin.paymentrisk.ops.application.payment.OpsPaymentSearchResult;
import dev.kavrin.paymentrisk.ops.application.payment.OpsPaymentSearchService;
import dev.kavrin.paymentrisk.payment.domain.model.PaymentStatus;
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
        controllers = OpsPaymentController.class,
        excludeAutoConfiguration = ReactiveWebSecurityAutoConfiguration.class
)
@Import({
        CorrelationIdWebFilter.class,
        GlobalApiExceptionHandler.class,
        OpsPaymentSearchResponseMapper.class,
        OpsPaymentControllerTest.TestOpsPaymentSearchConfiguration.class
})
class OpsPaymentControllerTest {

    private static final String PATH = OpsApiPaths.OPS_API_V1 + "/payments";
    private static final Instant CREATED_AT = Instant.parse("2026-06-04T10:00:00Z");

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private CapturingOpsPaymentSearchService paymentSearchService;

    @BeforeEach
    void resetService() {
        paymentSearchService.reset();
    }

    @Test
    void searchPaymentsDelegatesMappedRequestAndReturnsPage() {
        paymentSearchService.nextResult = new OpsPaymentSearchResult(
                List.of(searchItem("pay_ops_001", PaymentStatus.AUTHORIZED)),
                Optional.of("next_token_123")
        );

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(PATH)
                        .queryParam("status", "AUTHORIZED")
                        .queryParam("merchantId", "mer_ops_a")
                        .queryParam("customerId", "cus_ops_a")
                        .queryParam("paymentId", "pay_ops_001")
                        .queryParam("createdFrom", "2026-06-04T09:00:00Z")
                        .queryParam("createdTo", "2026-06-04T11:00:00Z")
                        .queryParam("size", "25")
                        .queryParam("pageToken", "opaque_page_token")
                        .build())
                .header(CorrelationIds.HEADER_NAME, "corr-ops-search")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(CorrelationIds.HEADER_NAME, "corr-ops-search")
                .expectBody()
                .jsonPath("$.items[0].paymentId").isEqualTo("pay_ops_001")
                .jsonPath("$.items[0].merchantId").isEqualTo("mer_ops_a")
                .jsonPath("$.items[0].customerId").isEqualTo("cus_ops_a")
                .jsonPath("$.items[0].amountMinor").isEqualTo(1299)
                .jsonPath("$.items[0].currency").isEqualTo("USD")
                .jsonPath("$.items[0].status").isEqualTo("AUTHORIZED")
                .jsonPath("$.items[0].externalReference").isEqualTo("order_pay_ops_001")
                .jsonPath("$.items[0].authorization.authorizationStatus").isEqualTo("AUTHORIZED")
                .jsonPath("$.items[0].authorization.authorizationCode").isEqualTo("AUTH-pay_ops_001")
                .jsonPath("$.items[0].risk.decision").isEqualTo("APPROVED")
                .jsonPath("$.items[0].risk.score").isEqualTo(12)
                .jsonPath("$.items[0].reversal.reversalId").isEqualTo("rev_pay_ops_001")
                .jsonPath("$.page.size").isEqualTo(1)
                .jsonPath("$.page.nextPageToken").isEqualTo("next_token_123")
                .jsonPath("$.page.hasNext").isEqualTo(true);

        OpsPaymentSearchRequest request = paymentSearchService.lastRequest.get();
        assertThat(request.status()).contains(PaymentStatus.AUTHORIZED);
        assertThat(request.merchantId().orElseThrow().value()).isEqualTo("mer_ops_a");
        assertThat(request.customerId().orElseThrow().value()).isEqualTo("cus_ops_a");
        assertThat(request.paymentId().orElseThrow().value()).isEqualTo("pay_ops_001");
        assertThat(request.createdFrom()).contains(Instant.parse("2026-06-04T09:00:00Z"));
        assertThat(request.createdTo()).contains(Instant.parse("2026-06-04T11:00:00Z"));
        assertThat(request.pageSize()).isEqualTo(25);
        assertThat(request.pageToken()).contains("opaque_page_token");
    }

    @Test
    void searchPaymentsReturnsEmptyPage() {
        paymentSearchService.nextResult = new OpsPaymentSearchResult(List.of(), Optional.empty());

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(PATH)
                        .queryParam("status", "DECLINED")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.items.length()").isEqualTo(0)
                .jsonPath("$.page.size").isEqualTo(0)
                .jsonPath("$.page.hasNext").isEqualTo(false)
                .jsonPath("$.page.nextPageToken").doesNotExist();
    }

    @Test
    void searchPaymentsReturnsValidationErrorForInvalidDateRange() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(PATH)
                        .queryParam("createdFrom", "2026-06-04T11:00:00Z")
                        .queryParam("createdTo", "2026-06-04T09:00:00Z")
                        .build())
                .header(CorrelationIds.HEADER_NAME, "corr-invalid-range")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_REQUEST")
                .jsonPath("$.message").isEqualTo("createdFrom must be before or equal to createdTo")
                .jsonPath("$.correlationId").isEqualTo("corr-invalid-range");
    }

    @Test
    void searchPaymentsReturnsValidationErrorForOversizedPage() {
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

    @Test
    void searchPaymentsReturnsMalformedRequestForInvalidStatus() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(PATH)
                        .queryParam("status", "not-a-status")
                        .build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("MALFORMED_REQUEST");
    }

    private static OpsPaymentSearchItem searchItem(String paymentId, PaymentStatus status) {
        return new OpsPaymentSearchItem(
                paymentId,
                "mer_ops_a",
                "cus_ops_a",
                1299,
                "USD",
                status,
                Optional.of("order_" + paymentId),
                Optional.of(new OpsPaymentSearchItem.AuthorizationSummary(
                        "AUTHORIZED",
                        Optional.of("AUTH-" + paymentId),
                        Optional.of(CREATED_AT.plusSeconds(1))
                )),
                Optional.of(new OpsPaymentSearchItem.RiskSummary(
                        "APPROVED",
                        12,
                        "risk-rules-v1",
                        CREATED_AT.plusSeconds(2)
                )),
                Optional.of(new OpsPaymentSearchItem.ReversalSummary(
                        "rev_" + paymentId,
                        "REVERSED",
                        "merchant_requested",
                        CREATED_AT.plusSeconds(3)
                )),
                CREATED_AT,
                CREATED_AT.plusSeconds(4)
        );
    }

    @TestConfiguration
    static class TestOpsPaymentSearchConfiguration {

        @Bean
        CapturingOpsPaymentSearchService paymentSearchService() {
            return new CapturingOpsPaymentSearchService();
        }
    }

    static class CapturingOpsPaymentSearchService implements OpsPaymentSearchService {
        final AtomicReference<OpsPaymentSearchRequest> lastRequest = new AtomicReference<>();
        OpsPaymentSearchResult nextResult = new OpsPaymentSearchResult(List.of(), Optional.empty());

        @Override
        public Mono<OpsPaymentSearchResult> search(OpsPaymentSearchRequest request) {
            lastRequest.set(request);
            return Mono.just(nextResult);
        }

        void reset() {
            lastRequest.set(null);
            nextResult = new OpsPaymentSearchResult(List.of(), Optional.empty());
        }
    }
}
