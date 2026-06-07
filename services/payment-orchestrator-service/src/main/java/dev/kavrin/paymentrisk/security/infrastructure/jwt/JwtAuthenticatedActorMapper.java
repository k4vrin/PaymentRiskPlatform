package dev.kavrin.paymentrisk.security.infrastructure.jwt;

import dev.kavrin.paymentrisk.security.application.AuthenticatedActor;
import dev.kavrin.paymentrisk.security.domain.ActorRole;
import dev.kavrin.paymentrisk.security.domain.ActorType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Maps validated JWT claims into the application-level authenticated actor model.
 */
@Component
public class JwtAuthenticatedActorMapper {

    public AuthenticatedActor map(Jwt jwt) {
        var roleClaims = jwt.getClaimAsStringList("roles");
        if (roleClaims == null || roleClaims.isEmpty()) {
            throw new InvalidBearerTokenException("JWT roles claim is required");
        }

        var roles = roleClaims
                .stream()
                .map(this::role)
                .collect(Collectors.toSet());

        var actorType = actorType(jwt.getClaimAsString("actor_type"));

        return new AuthenticatedActor(
                jwt.getSubject(),
                actorType,
                Set.copyOf(roles),
                jwt.getClaimAsString("merchant_id")
        );
    }

    private ActorRole role(String role) {
        try {
            return ActorRole.valueOf(role);
        } catch (IllegalArgumentException error) {
            throw new InvalidBearerTokenException("Unsupported JWT role: " + role, error);
        }
    }

    private ActorType actorType(String actorType) {
        if (actorType == null || actorType.isBlank()) {
            throw new InvalidBearerTokenException("JWT actor_type claim is required");
        }

        try {
            return ActorType.valueOf(actorType);
        } catch (IllegalArgumentException error) {
            throw new InvalidBearerTokenException("Unsupported JWT actor_type: " + actorType, error);
        }
    }
}
