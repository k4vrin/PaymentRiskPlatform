package dev.kavrin.paymentrisk.security.application;

import dev.kavrin.paymentrisk.security.domain.ActorRole;
import dev.kavrin.paymentrisk.security.domain.ActorType;

import java.util.Set;

/**
 * Application-level representation of the authenticated caller.
 *
 * <p>This model keeps business/application services independent from Spring
 * Security classes such as Authentication, Principal, and Jwt.</p>
 */
public record AuthenticatedActor(
        String actorId,
        ActorType actorType,
        Set<ActorRole> roles,
        String merchantId
) {

    public boolean hasRole(ActorRole role) {
        return roles != null && roles.contains(role);
    }
}
