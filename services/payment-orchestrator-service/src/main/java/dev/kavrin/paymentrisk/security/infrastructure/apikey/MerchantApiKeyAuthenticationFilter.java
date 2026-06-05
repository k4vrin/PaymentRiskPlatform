package dev.kavrin.paymentrisk.security.infrastructure.apikey;

import dev.kavrin.paymentrisk.security.application.InvalidMerchantApiKeyException;
import dev.kavrin.paymentrisk.security.application.MerchantApiKeyAuthenticator;
import dev.kavrin.paymentrisk.security.application.MerchantApiKeyParser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Authenticates merchant payment API requests using the X-API-Key header.
 */
@Component
@RequiredArgsConstructor
public class MerchantApiKeyAuthenticationFilter implements WebFilter {

    public static final String API_KEY_HEADER = "X-API-Key";

    private final MerchantApiKeyParser parser;
    private final MerchantApiKeyAuthenticator authenticator;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!isPaymentApi(exchange.getRequest())) {
            return chain.filter(exchange);
        }

        return Mono.defer(() -> {
                    var rawApiKey = exchange.getRequest()
                            .getHeaders()
                            .getFirst(API_KEY_HEADER);

                    var credential = parser.parse(rawApiKey);

                    return authenticator.authenticate(credential);
                })
                .flatMap(actor -> {
                    var authorities = actor.roles().stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                            .toList();

                    var authentication = new UsernamePasswordAuthenticationToken(
                            actor,
                            "N/A",
                            authorities
                    );

                    return chain.filter(exchange)
                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
                })
                .onErrorResume(
                        InvalidMerchantApiKeyException.class,
                        ignored -> {
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        }
                );
    }

    private static boolean isPaymentApi(ServerHttpRequest request) {
        var path = request.getPath().pathWithinApplication().value();

        return path.equals("/api/v1/payments/authorize")
                || path.matches("/api/v1/payments/[^/]+")
                || path.matches("/api/v1/payments/[^/]+/reverse");
    }
}
