package dev.kavrin.paymentrisk.security.infrastructure.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Converts a validated JWT into a Spring Security authentication token.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationConverter implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

    private final JwtAuthenticatedActorMapper actorMapper;

    @Override
    public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
        var actor = actorMapper.map(jwt);

        var authorities = actor.roles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .toList();

        return Mono.just(new UsernamePasswordAuthenticationToken(
                actor,
                jwt.getTokenValue(),
                authorities
        ));
    }
}