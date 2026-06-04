package dev.kavrin.paymentrisk.ops.api.consumerlag;

import dev.kavrin.paymentrisk.ops.application.consumerlag.ConsumerLagResult;
import dev.kavrin.paymentrisk.ops.application.consumerlag.ConsumerLagService;
import dev.kavrin.paymentrisk.shared.api.correlation.CorrelationIdWebFilter;
import dev.kavrin.paymentrisk.shared.api.error.GlobalApiExceptionHandler;
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
        controllers = OpsConsumerLagController.class,
        excludeAutoConfiguration = ReactiveWebSecurityAutoConfiguration.class
)
@Import({
        CorrelationIdWebFilter.class,
        GlobalApiExceptionHandler.class,
        OpsConsumerLagControllerTest.TestConsumerLagConfiguration.class
})
class OpsConsumerLagControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void returnsUnavailableConsumerLagStatus() {
        webTestClient.get()
                .uri("/api/v1/ops/consumer-lag?consumerGroup=payment-audit&topic=payment.authorization.completed")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.items.length()").isEqualTo(0)
                .jsonPath("$.unavailableReason").isEqualTo("Kafka consumer lag inspection is not configured yet.");
    }

    @TestConfiguration
    static class TestConsumerLagConfiguration {
        @Bean
        ConsumerLagService consumerLagService() {
            return request -> Mono.just(ConsumerLagResult.unavailable(
                    "Kafka consumer lag inspection is not configured yet."
            ));
        }
    }
}
