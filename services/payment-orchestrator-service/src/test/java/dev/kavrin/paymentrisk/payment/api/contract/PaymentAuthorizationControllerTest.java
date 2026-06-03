package dev.kavrin.paymentrisk.payment.api.contract;

import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKeyConflictException;
import dev.kavrin.paymentrisk.payment.application.command.AuthorizePaymentCommand;
import dev.kavrin.paymentrisk.payment.application.command.AuthorizePaymentResult;
import dev.kavrin.paymentrisk.payment.application.query.PaymentDetailsResult;
import dev.kavrin.paymentrisk.payment.application.query.PaymentLookupService;
import dev.kavrin.paymentrisk.payment.application.service.AuthorizePaymentService;
import dev.kavrin.paymentrisk.payment.domain.model.PaymentId;
import dev.kavrin.paymentrisk.shared.api.correlation.CorrelationIdWebFilter;
import dev.kavrin.paymentrisk.shared.api.correlation.CorrelationIds;
import dev.kavrin.paymentrisk.shared.api.error.DownstreamTimeoutException;
import dev.kavrin.paymentrisk.shared.api.error.GlobalApiExceptionHandler;
import dev.kavrin.paymentrisk.shared.api.error.ResourceNotFoundException;
import dev.kavrin.paymentrisk.shared.api.version.ApiPaths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.web.reactive.ReactiveWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@WebFluxTest(
        controllers = PaymentAuthorizationController.class,
        excludeAutoConfiguration = ReactiveWebSecurityAutoConfiguration.class
)
@Import({
        CorrelationIdWebFilter.class,
        GlobalApiExceptionHandler.class,
        PaymentAuthorizationControllerTest.TestAuthorizePaymentServiceConfiguration.class
})
class PaymentAuthorizationControllerTest {

    private static final String PATH = ApiPaths.API_V1 + "/payments/authorize";

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private CapturingAuthorizePaymentService authorizePaymentService;

    @Autowired
    private PaymentLookupService paymentLookupService;

    @BeforeEach
    void resetService() {
        authorizePaymentService.reset();
        reset(paymentLookupService);
    }

    @Test
    void authorizeDelegatesMappedCommandAndReturnsResponse() {
        webTestClient.post()
                .uri(PATH)
                .header(CorrelationIds.HEADER_NAME, "corr-controller")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "merchantId": "mer_01HX7Q9K2V6M8P4A3B9C1D2E3F",
                          "customerId": "cus_01HX7QAF4CQ8YFZ3M9N2W1P0VK",
                          "amountMinor": 1299,
                          "currency": "USD",
                          "paymentMethodToken": "pmt_tok_4f7b8d9c2a1e",
                          "deviceFingerprint": "dfp_6d9f1a2b3c4e5f678901",
                          "externalReference": "order_2026_000123",
                          "idempotencyKey": "idem_01HX7QK9JP7E5W5NRZ6T5Q3R1A"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(CorrelationIds.HEADER_NAME, "corr-controller")
                .expectBody()
                .jsonPath("$.paymentId").isEqualTo("pay_test")
                .jsonPath("$.status").isEqualTo("AUTHORIZED")
                .jsonPath("$.authorizationCode").isEqualTo("AUTH-ABCDEFG123")
                .jsonPath("$.riskDecision").isEqualTo("APPROVED")
                .jsonPath("$.reasonCodes[0]").isEqualTo("CONTRACT_ONLY_APPROVAL")
                .jsonPath("$.correlationId").isEqualTo("corr-controller")
                .jsonPath("$.riskScore").isEqualTo(0)
                .jsonPath("$.ruleVersion").isEqualTo("contract-only-v1");

        AuthorizePaymentCommand command = authorizePaymentService.lastCommand.get();
        assertThat(command.merchantId()).isEqualTo("mer_01HX7Q9K2V6M8P4A3B9C1D2E3F");
        assertThat(command.customerId()).isEqualTo("cus_01HX7QAF4CQ8YFZ3M9N2W1P0VK");
        assertThat(command.amountMinor()).isEqualTo(1299);
        assertThat(command.currency()).isEqualTo("USD");
        assertThat(command.paymentMethodToken()).isEqualTo("pmt_tok_4f7b8d9c2a1e");
        assertThat(command.deviceFingerprint()).isEqualTo("dfp_6d9f1a2b3c4e5f678901");
        assertThat(command.externalReference()).isEqualTo("order_2026_000123");
        assertThat(command.idempotencyKey()).isEqualTo("idem_01HX7QK9JP7E5W5NRZ6T5Q3R1A");
        assertThat(command.correlationId()).isEqualTo("corr-controller");
    }

    @Test
    void authorizeReturnsValidationErrorWhenIdempotencyKeyIsMissing() {
        webTestClient.post()
                .uri(PATH)
                .header(CorrelationIds.HEADER_NAME, "corr-validation")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "merchantId": "mer_01HX7Q9K2V6M8P4A3B9C1D2E3F",
                          "customerId": "cus_01HX7QAF4CQ8YFZ3M9N2W1P0VK",
                          "amountMinor": 1299,
                          "currency": "USD",
                          "paymentMethodToken": "pmt_tok_4f7b8d9c2a1e",
                          "deviceFingerprint": "dfp_6d9f1a2b3c4e5f678901",
                          "externalReference": "order_2026_000123"
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().valueEquals(CorrelationIds.HEADER_NAME, "corr-validation")
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_FAILED")
                .jsonPath("$.fieldErrors[0].field").isEqualTo("idempotencyKey");
    }

    @Test
    void authorizeReturnsValidationErrorForInvalidRequest() {
        webTestClient.post()
                .uri(PATH)
                .header(CorrelationIds.HEADER_NAME, "corr-invalid")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "merchantId": "mer_01HX7Q9K2V6M8P4A3B9C1D2E3F",
                          "customerId": "cus_01HX7QAF4CQ8YFZ3M9N2W1P0VK",
                          "amountMinor": 0,
                          "currency": "usd",
                          "paymentMethodToken": "pmt_tok_4f7b8d9c2a1e",
                          "deviceFingerprint": "dfp_6d9f1a2b3c4e5f678901",
                          "idempotencyKey": "idem_01HX7QK9JP7E5W5NRZ6T5Q3R1A"
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void authorizeReturnsStoredResponseForDuplicateReplay() {
        AuthorizePaymentResult storedResult = new AuthorizePaymentResult(
                "pay_stored",
                "AUTHORIZED",
                "AUTH-STORED1234",
                "APPROVED",
                List.of("LOW_RISK"),
                "corr-duplicate",
                11,
                "risk-rules-v1",
                Instant.parse("2026-05-25T10:14:30Z")
        );
        authorizePaymentService.nextResult = storedResult;

        webTestClient.post()
                .uri(PATH)
                .header(CorrelationIds.HEADER_NAME, "corr-duplicate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validRequestJson())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.paymentId").isEqualTo("pay_stored")
                .jsonPath("$.status").isEqualTo("AUTHORIZED")
                .jsonPath("$.authorizationCode").isEqualTo("AUTH-STORED1234")
                .jsonPath("$.riskDecision").isEqualTo("APPROVED")
                .jsonPath("$.correlationId").isEqualTo("corr-duplicate");
    }

    @Test
    void authorizeReturnsConflictForIdempotencyKeyReuseWithDifferentRequest() {
        authorizePaymentService.nextError = new IdempotencyKeyConflictException();

        webTestClient.post()
                .uri(PATH)
                .header(CorrelationIds.HEADER_NAME, "corr-conflict")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validRequestJson())
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("IDEMPOTENCY_KEY_CONFLICT")
                .jsonPath("$.correlationId").isEqualTo("corr-conflict");
    }

    @Test
    void authorizeReturnsGatewayTimeoutWhenRiskServiceTimesOut() {
        authorizePaymentService.nextError = new DownstreamTimeoutException("Risk service timed out");

        webTestClient.post()
                .uri(PATH)
                .header(CorrelationIds.HEADER_NAME, "corr-timeout")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validRequestJson())
                .exchange()
                .expectStatus().isEqualTo(504)
                .expectBody()
                .jsonPath("$.code").isEqualTo("RISK_SERVICE_TIMEOUT")
                .jsonPath("$.message").isEqualTo("Risk service timed out")
                .jsonPath("$.correlationId").isEqualTo("corr-timeout");
    }

    private static String validRequestJson() {
        return """
                {
                  "merchantId": "mer_01HX7Q9K2V6M8P4A3B9C1D2E3F",
                  "customerId": "cus_01HX7QAF4CQ8YFZ3M9N2W1P0VK",
                  "amountMinor": 1299,
                  "currency": "USD",
                  "paymentMethodToken": "pmt_tok_4f7b8d9c2a1e",
                  "deviceFingerprint": "dfp_6d9f1a2b3c4e5f678901",
                  "externalReference": "order_2026_000123",
                  "idempotencyKey": "idem_01HX7QK9JP7E5W5NRZ6T5Q3R1A"
                }
                """;
    }

    @TestConfiguration
    static class TestAuthorizePaymentServiceConfiguration {

        @Bean
        CapturingAuthorizePaymentService authorizePaymentService() {
            return new CapturingAuthorizePaymentService();
        }

        @Bean
        PaymentLookupService paymentLookupService() {
            return mock(PaymentLookupService.class);
        }
    }

    static class CapturingAuthorizePaymentService implements AuthorizePaymentService {

        private final AtomicReference<AuthorizePaymentCommand> lastCommand = new AtomicReference<>();
        private AuthorizePaymentResult nextResult;
        private RuntimeException nextError;

        void reset() {
            lastCommand.set(null);
            nextResult = null;
            nextError = null;
        }

        @Override
        public Mono<AuthorizePaymentResult> authorize(AuthorizePaymentCommand command) {
            lastCommand.set(command);

            if (nextError != null) {
                return Mono.error(nextError);
            }

            if (nextResult != null) {
                return Mono.just(nextResult);
            }

            return Mono.just(new AuthorizePaymentResult(
                    "pay_test",
                    "AUTHORIZED",
                    "AUTH-ABCDEFG123",
                    "APPROVED",
                    List.of("CONTRACT_ONLY_APPROVAL"),
                    command.correlationId(),
                    0,
                    "contract-only-v1",
                    Instant.parse("2026-05-25T10:15:30Z")
            ));
        }
    }

    @Test
    void getPaymentReturnsPaymentDetails() {
        var result = new PaymentDetailsResult(
                "pay_123",
                "merchant_123",
                "customer_123",
                10_000,
                "USD",
                "AUTHORIZED",
                "order_123",
                new PaymentDetailsResult.AuthorizationDetails(
                        "AUTHORIZED",
                        "auth_123",
                        Instant.parse("2026-06-01T10:00:00Z"),
                        Instant.parse("2026-06-01T10:00:01Z"),
                        Instant.parse("2026-06-01T10:00:02Z"),
                        null,
                        null
                ),
                new PaymentDetailsResult.RiskDetails(
                        "APPROVED",
                        10,
                        List.of("LOW_RISK"),
                        "rules-v1",
                        Instant.parse("2026-06-01T10:00:02Z")
                ),
                new PaymentDetailsResult.ReversalDetails(
                        "rev_123",
                        "REVERSED",
                        "merchant_requested",
                        Instant.parse("2026-06-01T10:02:00Z"),
                        Instant.parse("2026-06-01T10:02:01Z")
                ),
                Instant.parse("2026-06-01T10:00:00Z"),
                Instant.parse("2026-06-01T10:01:00Z")
        );

        when(paymentLookupService.getPaymentDetails(PaymentId.of("pay_123")))
                .thenReturn(Mono.just(result));

        webTestClient.get()
                .uri("/api/v1/payments/pay_123")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.paymentId").isEqualTo("pay_123")
                .jsonPath("$.status").isEqualTo("AUTHORIZED")
                .jsonPath("$.authorization.authorizationCode").isEqualTo("auth_123")
                .jsonPath("$.risk.decision").isEqualTo("APPROVED")
                .jsonPath("$.risk.reasonCodes[0]").isEqualTo("LOW_RISK")
                .jsonPath("$.reversal.reversalId").isEqualTo("rev_123")
                .jsonPath("$.reversal.status").isEqualTo("REVERSED")
                .jsonPath("$.paymentMethodToken").doesNotExist()
                .jsonPath("$.deviceFingerprint").doesNotExist();
    }

    @Test
    void getPaymentReturnsStructuredNotFoundWhenPaymentIsMissing() {
        when(paymentLookupService.getPaymentDetails(PaymentId.of("pay_missing")))
                .thenReturn(Mono.error(new ResourceNotFoundException("Payment not found: pay_missing")));

        webTestClient.get()
                .uri("/api/v1/payments/pay_missing")
                .header(CorrelationIds.HEADER_NAME, "corr-lookup-missing")
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().valueEquals(CorrelationIds.HEADER_NAME, "corr-lookup-missing")
                .expectBody()
                .jsonPath("$.code").isEqualTo("RESOURCE_NOT_FOUND")
                .jsonPath("$.message").isEqualTo("Payment not found: pay_missing")
                .jsonPath("$.correlationId").isEqualTo("corr-lookup-missing");
    }

    @Test
    void getPaymentReturnsValidationErrorForMalformedPaymentId() {
        webTestClient.get()
                .uri("/api/v1/payments/not-a-payment-id")
                .header(CorrelationIds.HEADER_NAME, "corr-lookup-invalid")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().valueEquals(CorrelationIds.HEADER_NAME, "corr-lookup-invalid")
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_REQUEST")
                .jsonPath("$.message")
                .isEqualTo("paymentId must start with pay_ and contain only letters, numbers, underscore, and hyphen.")
                .jsonPath("$.correlationId").isEqualTo("corr-lookup-invalid");
    }

}
