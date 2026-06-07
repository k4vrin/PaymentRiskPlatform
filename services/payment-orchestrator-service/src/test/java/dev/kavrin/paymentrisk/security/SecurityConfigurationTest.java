package dev.kavrin.paymentrisk.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import dev.kavrin.paymentrisk.security.application.*;
import dev.kavrin.paymentrisk.security.domain.ActorRole;
import dev.kavrin.paymentrisk.security.domain.ActorType;
import dev.kavrin.paymentrisk.security.infrastructure.apikey.MerchantApiKeyAuthenticationFilter;
import dev.kavrin.paymentrisk.security.infrastructure.jwt.JwtAuthenticatedActorMapper;
import dev.kavrin.paymentrisk.security.infrastructure.jwt.JwtAuthenticationConverter;
import dev.kavrin.paymentrisk.security.infrastructure.jwt.JwtDecoderConfiguration;
import dev.kavrin.paymentrisk.security.infrastructure.jwt.JwtSecurityConfiguration;
import dev.kavrin.paymentrisk.security.ratelimit.RateLimitDecision;
import dev.kavrin.paymentrisk.security.ratelimit.RateLimitProperties;
import dev.kavrin.paymentrisk.security.ratelimit.RateLimitWebFilter;
import dev.kavrin.paymentrisk.security.ratelimit.RateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Date;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@WebFluxTest(
        controllers = SecurityConfigurationTest.TestSecurityController.class,
        properties = {
                "payment-risk.security.jwt.issuer=test-issuer",
                "payment-risk.security.jwt.audience=payment-risk-platform-test",
                "payment-risk.security.jwt.secret=test-jwt-secret-must-be-at-least-32-bytes",
                "payment-risk.security.jwt.allowed-origins[0]=http://localhost:3000",
                "payment-risk.security.jwt.allowed-origins[1]=http://localhost:5173",
                "payment-risk.security.jwt.allowed-methods[0]=GET",
                "payment-risk.security.jwt.allowed-methods[1]=POST",
                "payment-risk.security.jwt.allowed-methods[2]=OPTIONS",
                "payment-risk.security.jwt.allowed-headers[0]=Authorization",
                "payment-risk.security.jwt.allowed-headers[1]=Content-Type",
                "payment-risk.security.jwt.allowed-headers[2]=X-API-Key",
                "payment-risk.security.jwt.allowed-headers[3]=X-Correlation-Id",
                "payment-risk.security.jwt.allowed-headers[4]=Idempotency-Key"
        }
)
@Import({
        JwtSecurityConfiguration.class,
        JwtDecoderConfiguration.class,
        JwtAuthenticationConverter.class,
        JwtAuthenticatedActorMapper.class,
        HeaderRoleAuthenticationWebFilter.class,
        MerchantApiKeyAuthenticationFilter.class,
        MerchantApiKeyParser.class,
        SecurityConfigurationTest.TestControllerConfiguration.class
})
class SecurityConfigurationTest {

    private static final String JWT_SECRET = "test-jwt-secret-must-be-at-least-32-bytes";

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

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
    void allowsOpsJwtForOpsApi() throws Exception {
        getWithBearer("/api/v1/ops/security-test", jwt("ops-user", ActorType.OPERATOR, Set.of(ActorRole.OPS)))
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("ops-user");
    }

    @Test
    void allowsAdminJwtForOpsApi() throws Exception {
        getWithBearer("/api/v1/ops/security-test", jwt("admin-user", ActorType.OPERATOR, Set.of(ActorRole.ADMIN)))
                .expectStatus().isOk();
    }

    @Test
    void deniesAuditorJwtForOpsApi() throws Exception {
        getWithBearer("/api/v1/ops/security-test", jwt("auditor-user", ActorType.OPERATOR, Set.of(ActorRole.AUDITOR)))
                .expectStatus().isForbidden();
    }

    @Test
    void rejectsInvalidJwtSignature() throws Exception {
        getWithBearer(
                "/api/v1/ops/security-test",
                jwt("ops-user", ActorType.OPERATOR, Set.of(ActorRole.OPS), "wrong-secret-must-be-at-least-32-bytes", "test-issuer", "payment-risk-platform-test", Instant.now().plusSeconds(300))
        ).expectStatus().isUnauthorized();
    }

    @Test
    void rejectsExpiredJwt() throws Exception {
        getWithBearer(
                "/api/v1/ops/security-test",
                jwt("ops-user", ActorType.OPERATOR, Set.of(ActorRole.OPS), JWT_SECRET, "test-issuer", "payment-risk-platform-test", Instant.now().minusSeconds(30))
        ).expectStatus().isUnauthorized();
    }

    @Test
    void rejectsWrongAudienceJwt() throws Exception {
        getWithBearer(
                "/api/v1/ops/security-test",
                jwt("ops-user", ActorType.OPERATOR, Set.of(ActorRole.OPS), JWT_SECRET, "test-issuer", "wrong-audience", Instant.now().plusSeconds(300))
        ).expectStatus().isUnauthorized();
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
    void allowsServiceJwtForInternalApis() throws Exception {
        getWithBearer("/api/v1/internal/security-test", jwt("service-client", ActorType.SERVICE, Set.of(ActorRole.SERVICE)))
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("service-client");
    }

    @Test
    void deniesOpsJwtForInternalApis() throws Exception {
        getWithBearer("/api/v1/internal/security-test", jwt("ops-user", ActorType.OPERATOR, Set.of(ActorRole.OPS)))
                .expectStatus().isForbidden();
    }

    @Test
    void permitsNonOpsEndpoints() {
        webTestClient.get()
                .uri("/public-test")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void addsSecurityHeaders() {
        webTestClient.get()
                .uri("/public-test")
                .exchange()
                .expectHeader().valueEquals("X-Frame-Options", "DENY")
                .expectHeader().valueEquals("X-Content-Type-Options", "nosniff")
                .expectHeader().exists("Content-Security-Policy");
    }

    @Test
    void allowsConfiguredCorsPreflight() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.options("/api/v1/ops/security-test")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization")
        );
        var configuration = corsConfigurationSource.getCorsConfiguration(exchange);

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOriginPatterns()).contains("http://localhost:3000");
        assertThat(configuration.getAllowedMethods()).contains("GET", "POST", "OPTIONS");
        assertThat(configuration.getAllowedHeaders()).contains("Authorization");
    }

    @Test
    void corsConfigurationRestrictsAllowedHeaders() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.options("/api/v1/ops/security-test")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET")
        );

        var configuration = corsConfigurationSource.getCorsConfiguration(exchange);

        assert configuration != null;
        assertThat(configuration.getAllowedHeaders())
                .contains("Authorization", "X-Correlation-Id", "X-API-Key", "Idempotency-Key");
    }

    @Test
    void rejectsUnconfiguredCorsPreflight() {
        webTestClient.options()
                .uri("/api/v1/ops/security-test")
                .header("Origin", "https://evil.example")
                .header("Access-Control-Request-Method", "GET")
                .exchange()
                .expectStatus().isForbidden();
    }

    private WebTestClient.ResponseSpec getWithRole(String uri, String userId, String role) {
        return webTestClient.get()
                .uri(uri)
                .header(HeaderRoleAuthenticationWebFilter.USER_ID_HEADER, userId)
                .header(HeaderRoleAuthenticationWebFilter.USER_ROLES_HEADER, role)
                .exchange();
    }

    private WebTestClient.ResponseSpec getWithBearer(String uri, String jwt) {
        return webTestClient.get()
                .uri(uri)
                .headers(headers -> headers.setBearerAuth(jwt))
                .exchange();
    }

    private static String jwt(String subject, ActorType actorType, Set<ActorRole> roles) throws Exception {
        return jwt(subject, actorType, roles, JWT_SECRET, "test-issuer", "payment-risk-platform-test", Instant.now().plusSeconds(300));
    }

    private static String jwt(
            String subject,
            ActorType actorType,
            Set<ActorRole> roles,
            String secret,
            String issuer,
            String audience,
            Instant expiresAt
    ) throws Exception {
        var claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .issuer(issuer)
                .audience(audience)
                .expirationTime(Date.from(expiresAt))
                .issueTime(Date.from(Instant.now()))
                .claim("actor_type", actorType.name())
                .claim("roles", roles.stream().map(ActorRole::name).toList())
                .build();

        var signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        signedJwt.sign(new MACSigner(secret.getBytes()));
        return signedJwt.serialize();
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

        @Bean
        RateLimitWebFilter rateLimitWebFilter() {
            RateLimiter limiter = ignored -> Mono.just(RateLimitDecision.allowed(100, 99));
            return new RateLimitWebFilter(
                    limiter,
                    new RateLimitProperties(false, 100, java.time.Duration.ofMinutes(1), "/api/v1/payments")
            );
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
            if (authentication.getPrincipal() instanceof AuthenticatedActor actor) {
                return Mono.just(actor.actorId());
            }

            return Mono.just(authentication.getName());
        }

        @GetMapping("/api/v1/audit/security-test")
        Mono<String> audit(Authentication authentication) {
            if (authentication.getPrincipal() instanceof AuthenticatedActor actor) {
                return Mono.just(actor.actorId());
            }

            return Mono.just(authentication.getName());
        }

        @GetMapping("/api/v1/internal/security-test")
        Mono<String> internal(Authentication authentication) {
            if (authentication.getPrincipal() instanceof AuthenticatedActor actor) {
                return Mono.just(actor.actorId());
            }

            return Mono.just(authentication.getName());
        }

        @GetMapping("/public-test")
        Mono<String> publicEndpoint() {
            return Mono.just("ok");
        }
    }
}
