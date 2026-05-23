package dev.kavrin.paymentrisk.shared.api.error;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.boot.security.autoconfigure.web.reactive.ReactiveWebSecurityAutoConfiguration;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.beans.factory.annotation.Autowired;

@WebFluxTest(
        controllers = TestErrorController.class,
        excludeAutoConfiguration = ReactiveWebSecurityAutoConfiguration.class
)
@Import(GlobalApiExceptionHandler.class)
class GlobalApiExceptionHandlerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void validationFailureReturnsStructuredErrorResponse() {
        webTestClient.post()
                .uri("/test/validate")
                .header("X-Correlation-Id", "corr-validation")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.code").isEqualTo("VALIDATION_FAILED")
                .jsonPath("$.message").isEqualTo("Request validation failed.")
                .jsonPath("$.path").isEqualTo("/test/validate")
                .jsonPath("$.correlationId").isEqualTo("corr-validation")
                .jsonPath("$.fieldErrors[0].field").isEqualTo("name")
                .jsonPath("$.fieldErrors[0].message").exists();
    }

    @Test
    void malformedRequestReturnsStructuredErrorResponse() {
        webTestClient.post()
                .uri("/test/validate")
                .header("X-Correlation-Id", "corr-malformed")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.code").isEqualTo("MALFORMED_REQUEST")
                .jsonPath("$.path").isEqualTo("/test/validate")
                .jsonPath("$.correlationId").isEqualTo("corr-malformed");
    }

    @Test
    void notFoundExceptionReturnsStructuredErrorResponse() {
        webTestClient.get()
                .uri("/test/not-found")
                .header("X-Correlation-Id", "corr-not-found")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.code").isEqualTo("RESOURCE_NOT_FOUND")
                .jsonPath("$.message").isEqualTo("Resource was not found.")
                .jsonPath("$.path").isEqualTo("/test/not-found")
                .jsonPath("$.correlationId").isEqualTo("corr-not-found")
                .jsonPath("$.fieldErrors").isArray();
    }

    @Test
    void conflictExceptionReturnsStructuredErrorResponse() {
        webTestClient.get()
                .uri("/test/conflict")
                .header("X-Correlation-Id", "corr-conflict")
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.status").isEqualTo(409)
                .jsonPath("$.code").isEqualTo("PAYMENT_STATE_CONFLICT")
                .jsonPath("$.message").isEqualTo("Payment state does not allow this operation.")
                .jsonPath("$.path").isEqualTo("/test/conflict")
                .jsonPath("$.correlationId").isEqualTo("corr-conflict");
    }

    @Test
    void downstreamTimeoutExceptionReturnsStructuredErrorResponse() {
        webTestClient.get()
                .uri("/test/timeout")
                .header("X-Correlation-Id", "corr-timeout")
                .exchange()
                .expectStatus().isEqualTo(504)
                .expectBody()
                .jsonPath("$.status").isEqualTo(504)
                .jsonPath("$.code").isEqualTo("RISK_SERVICE_TIMEOUT")
                .jsonPath("$.message").isEqualTo("Risk service timed out.")
                .jsonPath("$.path").isEqualTo("/test/timeout")
                .jsonPath("$.correlationId").isEqualTo("corr-timeout");
    }

    @Test
    void unexpectedExceptionReturnsStructuredErrorResponse() {
        webTestClient.get()
                .uri("/test/unexpected")
                .header("X-Correlation-Id", "corr-internal")
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.status").isEqualTo(500)
                .jsonPath("$.code").isEqualTo("INTERNAL_ERROR")
                .jsonPath("$.message").isEqualTo("An unexpected error occurred.")
                .jsonPath("$.path").isEqualTo("/test/unexpected")
                .jsonPath("$.correlationId").isEqualTo("corr-internal");
    }
}
