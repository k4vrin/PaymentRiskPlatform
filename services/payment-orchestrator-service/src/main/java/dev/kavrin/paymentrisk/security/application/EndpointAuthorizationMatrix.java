package dev.kavrin.paymentrisk.security.application;

import dev.kavrin.paymentrisk.security.domain.ActorRole;
import dev.kavrin.paymentrisk.security.domain.EndpointGroup;

import java.util.Map;
import java.util.Set;

/**
 * Central source of truth for endpoint-level role access.
 *
 * <p>This class does not perform authentication. It only documents and exposes
 * which authenticated roles are allowed to access each endpoint group.</p>
 */
public final class EndpointAuthorizationMatrix {

    private static final Map<EndpointGroup, Set<ActorRole>> ALLOWED_ROLES = Map.of(
            EndpointGroup.PAYMENT_API, Set.of(ActorRole.MERCHANT),
            EndpointGroup.OPS_API, Set.of(ActorRole.OPS, ActorRole.ADMIN),
            EndpointGroup.AUDIT_READ_API, Set.of(ActorRole.AUDITOR, ActorRole.OPS, ActorRole.ADMIN),
            EndpointGroup.INTERNAL_SERVICE_API, Set.of(ActorRole.SERVICE, ActorRole.ADMIN),
            EndpointGroup.HEALTH_API, Set.of()
    );

    private EndpointAuthorizationMatrix() {
    }

    public static Set<ActorRole> allowedRoles(EndpointGroup endpointGroup) {
        return ALLOWED_ROLES.getOrDefault(endpointGroup, Set.of());
    }

    public static boolean isAllowed(EndpointGroup endpointGroup, ActorRole role) {
        return allowedRoles(endpointGroup).contains(role);
    }
}
