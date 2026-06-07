package dev.kavrin.paymentrisk.security.ratelimit;

import dev.kavrin.paymentrisk.security.application.AuthenticatedActor;
import dev.kavrin.paymentrisk.security.domain.ActorRole;
import dev.kavrin.paymentrisk.security.domain.ActorType;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitWebFilterTest {

    private static final RateLimitProperties PROPERTIES = new RateLimitProperties(
            true,
            2,
            Duration.ofMinutes(1),
            "/api/v1/payments"
    );

    @Test
    void allowsRequestAndUsesMerchantIdentityAsRateLimitKey() {
        var keys = new ArrayList<String>();
        RateLimiter limiter = key -> {
            keys.add(key);
            return Mono.just(RateLimitDecision.allowed(2, 1));
        };
        var filter = new RateLimitWebFilter(limiter, PROPERTIES);
        var exchange = paymentExchange();
        var chainReached = new AtomicBoolean(false);
        WebFilterChain chain = ignored -> {
            chainReached.set(true);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(merchantAuthentication())))
                .verifyComplete();

        assertThat(keys).containsExactly("merchant:merchant_123");
        assertThat(chainReached).isTrue();
        assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit")).isEqualTo("2");
        assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Remaining")).isEqualTo("1");
    }

    @Test
    void rejectsExceededRequestWithRetryAfterHeader() {
        RateLimiter limiter = ignored -> Mono.just(RateLimitDecision.rejected(2, Duration.ofSeconds(30)));
        var filter = new RateLimitWebFilter(limiter, PROPERTIES);
        var exchange = paymentExchange();
        var chainReached = new AtomicBoolean(false);

        StepVerifier.create(filter.filter(exchange, ignored -> {
                            chainReached.set(true);
                            return Mono.empty();
                        })
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(merchantAuthentication())))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(RateLimitExceededException.class)
                        .hasMessage("Request rate limit exceeded"))
                .verify();

        assertThat(chainReached).isFalse();
        assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit")).isEqualTo("2");
        assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Remaining")).isEqualTo("0");
        assertThat(exchange.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("30");
    }

    @Test
    void usesClientIpWhenAuthenticationIsMissing() {
        var keys = new ArrayList<String>();
        RateLimiter limiter = key -> {
            keys.add(key);
            return Mono.just(RateLimitDecision.allowed(2, 1));
        };
        var filter = new RateLimitWebFilter(limiter, PROPERTIES);
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/payments/authorize")
                .remoteAddress(new InetSocketAddress("203.0.113.9", 443)));

        StepVerifier.create(filter.filter(exchange, ignored -> Mono.empty()))
                .verifyComplete();

        assertThat(keys).containsExactly("merchant:203.0.113.9");
    }

    @Test
    void skipsWhenRateLimitingIsDisabled() {
        var keys = new ArrayList<String>();
        RateLimiter limiter = key -> {
            keys.add(key);
            return Mono.just(RateLimitDecision.rejected(2, Duration.ofSeconds(30)));
        };
        var disabledProperties = new RateLimitProperties(false, 2, Duration.ofMinutes(1), "/api/v1/payments");
        var filter = new RateLimitWebFilter(limiter, disabledProperties);
        var exchange = paymentExchange();
        var chainReached = new AtomicBoolean(false);

        StepVerifier.create(filter.filter(exchange, ignored -> {
            chainReached.set(true);
            return Mono.empty();
        })).verifyComplete();

        assertThat(keys).isEmpty();
        assertThat(chainReached).isTrue();
    }

    @Test
    void skipsRequestsOutsideConfiguredPath() {
        var keys = new ArrayList<String>();
        RateLimiter limiter = key -> {
            keys.add(key);
            return Mono.just(RateLimitDecision.rejected(2, Duration.ofSeconds(30)));
        };
        var filter = new RateLimitWebFilter(limiter, PROPERTIES);
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/ops/payments"));
        var chainReached = new AtomicBoolean(false);

        StepVerifier.create(filter.filter(exchange, ignored -> {
            chainReached.set(true);
            return Mono.empty();
        })).verifyComplete();

        assertThat(keys).isEmpty();
        assertThat(chainReached).isTrue();
    }

    private static MockServerWebExchange paymentExchange() {
        return MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/payments/authorize"));
    }

    private static UsernamePasswordAuthenticationToken merchantAuthentication() {
        var actor = new AuthenticatedActor(
                "merchant_123",
                ActorType.MERCHANT,
                Set.of(ActorRole.MERCHANT),
                "merchant_123"
        );

        return new UsernamePasswordAuthenticationToken(
                actor,
                "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_MERCHANT"))
        );
    }
}
