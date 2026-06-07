package dev.kavrin.paymentrisk.security.ratelimit;

import dev.kavrin.paymentrisk.security.application.AuthenticatedActor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Applies merchant-level rate limiting to payment APIs.
 *
 * <p>The merchant identity comes from Spring Security authentication.
 * This keeps the limiter independent of the API-key implementation details.</p>
 */
@RequiredArgsConstructor
public class RateLimitWebFilter implements WebFilter {

    private static final String RATE_LIMIT_HEADER = "X-RateLimit-Limit";
    private static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";
    private static final String RETRY_AFTER_HEADER = HttpHeaders.RETRY_AFTER;

    private final RateLimiter rateLimiter;
    private final RateLimitProperties properties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!shouldRateLimit(exchange)) {
            return chain.filter(exchange);
        }

        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> rateLimitIdentity(securityContext.getAuthentication(), exchange))
                .switchIfEmpty(Mono.just(resolveClientIp(exchange)))
                .map(this::merchantRateLimitKey)
                .flatMap(rateLimiter::check)
                .flatMap(decision -> applyDecision(exchange, chain, decision));
    }

    private boolean shouldRateLimit(ServerWebExchange exchange) {
        return properties.enabled()
                && exchange.getRequest().getPath().value().startsWith(properties.pathPrefix());
    }

    private String merchantRateLimitKey(String merchantIdentity) {
        return "merchant:" + merchantIdentity;
    }

    private String rateLimitIdentity(Authentication authentication, ServerWebExchange exchange) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return resolveClientIp(exchange);
        }

        if (authentication.getPrincipal() instanceof AuthenticatedActor actor
                && actor.merchantId() != null
                && !actor.merchantId().isBlank()) {
            return actor.merchantId();
        }

        return authentication.getName();
    }

    private String resolveClientIp(ServerWebExchange exchange) {
        var remoteAddress = exchange.getRequest().getRemoteAddress();

        if (remoteAddress == null || remoteAddress.getAddress() == null) {
            return "unknown-client";
        }

        return remoteAddress.getAddress().getHostAddress();
    }

    private Mono<Void> applyDecision(
            ServerWebExchange exchange,
            WebFilterChain chain,
            RateLimitDecision decision
    ) {
        exchange.getResponse().getHeaders().add(RATE_LIMIT_HEADER, Long.toString(decision.limit()));
        exchange.getResponse().getHeaders().add(RATE_LIMIT_REMAINING_HEADER, Long.toString(decision.remaining()));

        if (decision.allowed()) {
            return chain.filter(exchange);
        }

        exchange.getResponse()
                .getHeaders()
                .add(RETRY_AFTER_HEADER, Long.toString(decision.retryAfter().toSeconds()));

        return Mono.error(new RateLimitExceededException(decision.limit(), decision.retryAfter()));
    }
}
