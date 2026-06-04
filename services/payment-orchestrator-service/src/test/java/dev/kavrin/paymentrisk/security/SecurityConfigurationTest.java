package dev.kavrin.paymentrisk.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@WebFluxTest(controllers = SecurityConfigurationTest.TestOpsController.class)
@Import({
        SecurityConfiguration.class,
        HeaderRoleAuthenticationWebFilter.class,
        SecurityConfigurationTest.TestControllerConfiguration.class
})
class SecurityConfigurationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void allowsOpsRole() {
        webTestClient.get()
                .uri("/api/v1/ops/security-test")
                .header(HeaderRoleAuthenticationWebFilter.USER_ID_HEADER, "ops-user")
                .header(HeaderRoleAuthenticationWebFilter.USER_ROLES_HEADER, "OPS")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("ops-user");
    }

    @Test
    void allowsAdminRole() {
        webTestClient.get()
                .uri("/api/v1/ops/security-test")
                .header(HeaderRoleAuthenticationWebFilter.USER_ID_HEADER, "admin-user")
                .header(HeaderRoleAuthenticationWebFilter.USER_ROLES_HEADER, "ADMIN")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void deniesMerchantRole() {
        webTestClient.get()
                .uri("/api/v1/ops/security-test")
                .header(HeaderRoleAuthenticationWebFilter.USER_ID_HEADER, "merchant-user")
                .header(HeaderRoleAuthenticationWebFilter.USER_ROLES_HEADER, "MERCHANT")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void deniesAnonymousRequest() {
        webTestClient.get()
                .uri("/api/v1/ops/security-test")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void permitsNonOpsEndpoints() {
        webTestClient.get()
                .uri("/public-test")
                .exchange()
                .expectStatus().isOk();
    }

    @TestConfiguration
    static class TestControllerConfiguration {
        @Bean
        TestOpsController testOpsController() {
            return new TestOpsController();
        }
    }

    @RestController
    static class TestOpsController {
        @GetMapping("/api/v1/ops/security-test")
        Mono<String> ops(Authentication authentication) {
            return Mono.just(authentication.getName());
        }

        @GetMapping("/public-test")
        Mono<String> publicEndpoint() {
            return Mono.just("ok");
        }
    }
}
