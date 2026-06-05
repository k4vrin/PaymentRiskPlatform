package dev.kavrin.paymentrisk.security;

import dev.kavrin.paymentrisk.security.infrastructure.apikey.MerchantApiKeyAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
class SecurityConfiguration {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(
            MerchantApiKeyAuthenticationFilter merchantApiKeyAuthenticationFilter,
            HeaderRoleAuthenticationWebFilter headerRoleAuthenticationWebFilter
    ) {
        return ServerHttpSecurity.http()
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .addFilterAt(merchantApiKeyAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .addFilterAt(headerRoleAuthenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/api/v1/payments/**").hasRole(SecurityRoles.MERCHANT)
                        .pathMatchers("/api/v1/ops/**").hasAnyRole(SecurityRoles.OPS, SecurityRoles.ADMIN)
                        .pathMatchers("/api/v1/audit/**")
                        .hasAnyRole(SecurityRoles.AUDITOR, SecurityRoles.OPS, SecurityRoles.ADMIN)
                        .pathMatchers("/api/v1/internal/**").hasAnyRole(SecurityRoles.SERVICE, SecurityRoles.ADMIN)
                        .pathMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .anyExchange().permitAll()
                )
                .build();
    }
}
