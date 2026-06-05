package dev.kavrin.paymentrisk.security.application;

import dev.kavrin.paymentrisk.security.domain.ActorRole;
import dev.kavrin.paymentrisk.security.domain.ActorType;
import dev.kavrin.paymentrisk.security.domain.MerchantApiKeyStatus;
import dev.kavrin.paymentrisk.security.infrastructure.apikey.MerchantApiKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * Authenticates merchant API keys and resolves them into application actors.
 */
@Service
@RequiredArgsConstructor
public class MerchantApiKeyAuthenticator {

    private final MerchantApiKeyRepository repository;
    private final MerchantApiKeyVerifier verifier;

    public Mono<AuthenticatedActor> authenticate(MerchantApiKeyCredential credential) {
        return repository.findByKeyId(credential.keyId())
                .switchIfEmpty(Mono.error(new InvalidMerchantApiKeyException("Unknown merchant API key")))
                .flatMap(entity -> {
                    if (!MerchantApiKeyStatus.ACTIVE.name().equals(entity.status())) {
                        return Mono.error(new InvalidMerchantApiKeyException("Merchant API key is not active"));
                    }

                    if (!verifier.matches(credential.keyId(), credential.secret(), entity.secretHash())) {
                        return Mono.error(new InvalidMerchantApiKeyException("Invalid merchant API key secret"));
                    }

                    return Mono.just(new AuthenticatedActor(
                            entity.merchantId(),
                            ActorType.MERCHANT,
                            Set.of(ActorRole.MERCHANT),
                            entity.merchantId()
                    ));
                });
    }
}