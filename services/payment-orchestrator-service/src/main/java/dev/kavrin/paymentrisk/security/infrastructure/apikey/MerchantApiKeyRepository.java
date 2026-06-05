package dev.kavrin.paymentrisk.security.infrastructure.apikey;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/**
 * Reactive repository for merchant API key lookup.
 */
public interface MerchantApiKeyRepository
        extends ReactiveCrudRepository<MerchantApiKeyEntity, Long> {

    Mono<MerchantApiKeyEntity> findByKeyId(String keyId);
}