package dev.kavrin.paymentrisk.security.application;

import dev.kavrin.paymentrisk.security.domain.ActorRole;
import dev.kavrin.paymentrisk.security.domain.EndpointGroup;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EndpointAuthorizationMatrixTest {

    @Test
    void paymentApisShouldRequireMerchantRole() {
        assertThat(EndpointAuthorizationMatrix.isAllowed(
                EndpointGroup.PAYMENT_API,
                ActorRole.MERCHANT
        )).isTrue();

        assertThat(EndpointAuthorizationMatrix.isAllowed(
                EndpointGroup.PAYMENT_API,
                ActorRole.OPS
        )).isFalse();

        assertThat(EndpointAuthorizationMatrix.isAllowed(
                EndpointGroup.PAYMENT_API,
                ActorRole.ADMIN
        )).isFalse();
    }

    @Test
    void opsApisShouldAllowOpsAndAdmin() {
        assertThat(EndpointAuthorizationMatrix.allowedRoles(EndpointGroup.OPS_API))
                .containsExactlyInAnyOrder(
                        ActorRole.OPS,
                        ActorRole.ADMIN
                );
    }

    @Test
    void auditReadApisShouldAllowAuditorOpsAndAdmin() {
        assertThat(EndpointAuthorizationMatrix.allowedRoles(EndpointGroup.AUDIT_READ_API))
                .containsExactlyInAnyOrder(
                        ActorRole.AUDITOR,
                        ActorRole.OPS,
                        ActorRole.ADMIN
                );
    }

    @Test
    void internalServiceApisShouldAllowServiceAndAdmin() {
        assertThat(EndpointAuthorizationMatrix.allowedRoles(EndpointGroup.INTERNAL_SERVICE_API))
                .containsExactlyInAnyOrder(
                        ActorRole.SERVICE,
                        ActorRole.ADMIN
                );
    }

    @Test
    void healthApiHasNoRequiredRoleInPolicyMatrix() {
        assertThat(EndpointAuthorizationMatrix.allowedRoles(EndpointGroup.HEALTH_API))
                .isEmpty();
    }
}
