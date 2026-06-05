package dev.kavrin.paymentrisk.security.application;

import reactor.core.publisher.Mono;

/**
 * Provides the currently authenticated actor to application services.
 */
public interface CurrentActorProvider {

    Mono<AuthenticatedActor> currentActor();
}
