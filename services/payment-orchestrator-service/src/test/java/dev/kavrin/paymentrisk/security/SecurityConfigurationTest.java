package dev.kavrin.paymentrisk.security;

import dev.kavrin.paymentrisk.security.application.*;
import dev.kavrin.paymentrisk.security.domain.ActorRole;
import dev.kavrin.paymentrisk.security.domain.ActorType;
import dev.kavrin.paymentrisk.security.infrastructure.apikey.MerchantApiKeyAuthenticationFilter;
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

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = SecurityConfigurationTest.TestSecurityController.class)
@Import({
        SecurityConfiguration.class,
        HeaderRoleAuthenticationWebFilter.class,
        MerchantApiKeyAuthenticationFilter.class,
        MerchantApiKeyParser.class,
        SecurityConfigurationTest.TestControllerConfiguration.class
})
class SecurityConfigurationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void allowsMerchantRoleForPaymentApis() {
        webTestClient.get()
                .uri("/api/v1/payments/security-test")
                .header(MerchantApiKeyAuthenticationFilter.API_KEY_HEADER, "key_live.secret_live")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("merchant_123");
    }

    @Test
    void deniesHeaderRoleFallbackForPaymentApis() {
        getWithRole("/api/v1/payments/security-test", "merchant-user", SecurityRoles.MERCHANT)
                .expectStatus().isUnauthorized();
    }

    @Test
    void deniesInvalidApiKeyForPaymentApis() {
        webTestClient.get()
                .uri("/api/v1/payments/security-test")
                .header(MerchantApiKeyAuthenticationFilter.API_KEY_HEADER, "key_live.wrong_secret")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void deniesMalformedApiKeyForPaymentApis() {
        webTestClient.get()
                .uri("/api/v1/payments/security-test")
                .header(MerchantApiKeyAuthenticationFilter.API_KEY_HEADER, "malformed")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void allowsOpsRole() {
        getWithRole("/api/v1/ops/security-test", "ops-user", SecurityRoles.OPS)
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("ops-user");
    }

    @Test
    void allowsAdminRole() {
        getWithRole("/api/v1/ops/security-test", "admin-user", SecurityRoles.ADMIN)
                .expectStatus().isOk();
    }

    @Test
    void deniesMerchantRole() {
        getWithRole("/api/v1/ops/security-test", "merchant-user", SecurityRoles.MERCHANT)
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
    void allowsAuditorOpsAndAdminForAuditReadApis() {
        getWithRole("/api/v1/audit/security-test", "auditor-user", SecurityRoles.AUDITOR)
                .expectStatus().isOk();
        getWithRole("/api/v1/audit/security-test", "ops-user", SecurityRoles.OPS)
                .expectStatus().isOk();
        getWithRole("/api/v1/audit/security-test", "admin-user", SecurityRoles.ADMIN)
                .expectStatus().isOk();
    }

    @Test
    void deniesMerchantRoleForAuditReadApis() {
        getWithRole("/api/v1/audit/security-test", "merchant-user", SecurityRoles.MERCHANT)
                .expectStatus().isForbidden();
    }

    @Test
    void allowsServiceAndAdminForInternalApis() {
        getWithRole("/api/v1/internal/security-test", "service-client", SecurityRoles.SERVICE)
                .expectStatus().isOk();
        getWithRole("/api/v1/internal/security-test", "admin-user", SecurityRoles.ADMIN)
                .expectStatus().isOk();
    }

    @Test
    void deniesOpsRoleForInternalApis() {
        getWithRole("/api/v1/internal/security-test", "ops-user", SecurityRoles.OPS)
                .expectStatus().isForbidden();
    }

    @Test
    void permitsNonOpsEndpoints() {
        webTestClient.get()
                .uri("/public-test")
                .exchange()
                .expectStatus().isOk();
    }

    private WebTestClient.ResponseSpec getWithRole(String uri, String userId, String role) {
        return webTestClient.get()
                .uri(uri)
                .header(HeaderRoleAuthenticationWebFilter.USER_ID_HEADER, userId)
                .header(HeaderRoleAuthenticationWebFilter.USER_ROLES_HEADER, role)
                .exchange();
    }

    @TestConfiguration
    static class TestControllerConfiguration {
        @Bean
        TestSecurityController testSecurityController() {
            return new TestSecurityController();
        }

        @Bean
        MerchantApiKeyAuthenticator merchantApiKeyAuthenticator() {
            var authenticator = mock(MerchantApiKeyAuthenticator.class);
            when(authenticator.authenticate(any()))
                    .thenReturn(Mono.error(new InvalidMerchantApiKeyException("Invalid merchant API key")));
            when(authenticator.authenticate(new MerchantApiKeyCredential("key_live", "secret_live")))
                    .thenReturn(Mono.just(new AuthenticatedActor(
                            "merchant_123",
                            ActorType.MERCHANT,
                            Set.of(ActorRole.MERCHANT),
                            "merchant_123"
                    )));
            return authenticator;
        }
    }

    @RestController
    static class TestSecurityController {
        @GetMapping("/api/v1/payments/security-test")
        Mono<String> payment(Authentication authentication) {
            var actor = (AuthenticatedActor) authentication.getPrincipal();
            return Mono.just(actor.merchantId());
        }

        @GetMapping("/api/v1/ops/security-test")
        Mono<String> ops(Authentication authentication) {
            return Mono.just(authentication.getName());
        }

        @GetMapping("/api/v1/audit/security-test")
        Mono<String> audit(Authentication authentication) {
            return Mono.just(authentication.getName());
        }

        @GetMapping("/api/v1/internal/security-test")
        Mono<String> internal(Authentication authentication) {
            return Mono.just(authentication.getName());
        }

        @GetMapping("/public-test")
        Mono<String> publicEndpoint() {
            return Mono.just("ok");
        }
    }
}
