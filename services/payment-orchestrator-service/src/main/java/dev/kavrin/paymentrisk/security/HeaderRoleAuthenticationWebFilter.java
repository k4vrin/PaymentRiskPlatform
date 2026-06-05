package dev.kavrin.paymentrisk.security;

import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Arrays;

@Component
@Order(-100)
public class HeaderRoleAuthenticationWebFilter implements WebFilter {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLES_HEADER = "X-User-Roles";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (isPaymentApi(exchange)) {
            return chain.filter(exchange);
        }

        var rolesHeader = exchange.getRequest().getHeaders().getFirst(USER_ROLES_HEADER);

        if (rolesHeader == null || rolesHeader.isBlank()) {
            return chain.filter(exchange);
        }

        var username = exchange.getRequest().getHeaders().getFirst(USER_ID_HEADER);
        if (username == null || username.isBlank()) {
            username = "local-ops-user";
        }

        var authorities = Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .toList();

        var authentication = new UsernamePasswordAuthenticationToken(
                username,
                "n/a",
                authorities
        );

        return chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
    }

    private static boolean isPaymentApi(ServerWebExchange exchange) {
        return exchange.getRequest()
                .getPath()
                .pathWithinApplication()
                .value()
                .startsWith("/api/v1/payments/");
    }
}
