package dev.kavrin.paymentrisk.security.infrastructure;

import dev.kavrin.paymentrisk.security.application.AuthenticatedActor;
import dev.kavrin.paymentrisk.security.application.CurrentActorProvider;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Reads the current authenticated actor from the reactive Spring Security context.
 */
@Component
public class SpringSecurityCurrentActorProvider implements CurrentActorProvider {

    @Override
    public Mono<AuthenticatedActor> currentActor() {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> Objects.requireNonNull(context.getAuthentication()).getPrincipal())
                .cast(AuthenticatedActor.class);
    }
}