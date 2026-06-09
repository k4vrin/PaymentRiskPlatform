package dev.kavrin.paymentrisk.security.infrastructure.jwt;

import dev.kavrin.paymentrisk.security.HeaderRoleAuthenticationWebFilter;
import dev.kavrin.paymentrisk.security.SecurityRoles;
import dev.kavrin.paymentrisk.security.infrastructure.apikey.MerchantApiKeyAuthenticationFilter;
import dev.kavrin.paymentrisk.security.ratelimit.RateLimitWebFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableConfigurationProperties(JwtSecurityProperties.class)
public class JwtSecurityConfiguration {

    @Bean
    SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http,
            MerchantApiKeyAuthenticationFilter apiKeyAuthenticationFilter,
            HeaderRoleAuthenticationWebFilter headerRoleAuthenticationWebFilter,
            RateLimitWebFilter rateLimitWebFilter,
            CorsConfigurationSource corsConfigurationSource,
            JwtAuthenticationConverter jwtAuthenticationConverter
    ) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .headers(headers -> headers
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(frameOptions -> frameOptions.mode(XFrameOptionsServerHttpHeadersWriter.Mode.DENY))
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; frame-ancestors 'none'; object-src 'none'; base-uri 'self'"
                        ))
                        .hsts(hsts -> hsts
                                .includeSubdomains(true)
                                .preload(true)
                                .maxAge(Duration.ofDays(365))
                        )
                )
                .addFilterAt(apiKeyAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .addFilterAt(headerRoleAuthenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .addFilterAt(rateLimitWebFilter, SecurityWebFiltersOrder.AUTHORIZATION)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers(
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/prometheus",
                                "/api/v1/contract/ping"
                        ).permitAll()
                        .pathMatchers("/api/v1/payments/**").hasRole(SecurityRoles.MERCHANT)
                        .pathMatchers("/api/v1/ops/**").hasAnyRole(SecurityRoles.OPS, SecurityRoles.ADMIN)
                        .pathMatchers("/api/v1/audit/**")
                        .hasAnyRole(SecurityRoles.AUDITOR, SecurityRoles.OPS, SecurityRoles.ADMIN)
                        .pathMatchers("/api/v1/internal/**").hasAnyRole(SecurityRoles.SERVICE, SecurityRoles.ADMIN)
                        .anyExchange().permitAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                )
                .build();
    }

    /**
     * Defines cross-origin access for browser clients.
     *
     * <p>Local development allows common frontend dev ports. Production should
     * override allowed origins through environment-specific configuration.</p>
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource(JwtSecurityProperties properties) {
        var configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(properties.getAllowedOrigins());
        configuration.setAllowedMethods(properties.getAllowedMethods());
        configuration.setAllowedHeaders(properties.getAllowedHeaders());

        configuration.setExposedHeaders(List.of("X-Correlation-Id"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    CorsWebFilter corsWebFilter(CorsConfigurationSource corsConfigurationSource) {
        return new CorsWebFilter(corsConfigurationSource);
    }

    @Bean
    WebFluxConfigurer corsWebFluxConfigurer(JwtSecurityProperties properties) {
        return new WebFluxConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns(properties.getAllowedOrigins().toArray(String[]::new))
                        .allowedMethods(properties.getAllowedMethods().toArray(String[]::new))
                        .allowedHeaders(properties.getAllowedHeaders().toArray(String[]::new))
                        .exposedHeaders("X-Correlation-Id")
                        .allowCredentials(false)
                        .maxAge(3600L);
            }
        };
    }
}
