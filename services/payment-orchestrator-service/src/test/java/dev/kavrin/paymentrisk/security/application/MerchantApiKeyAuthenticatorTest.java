package dev.kavrin.paymentrisk.security.application;

import dev.kavrin.paymentrisk.security.domain.ActorRole;
import dev.kavrin.paymentrisk.security.domain.ActorType;
import dev.kavrin.paymentrisk.security.infrastructure.apikey.MerchantApiKeyEntity;
import dev.kavrin.paymentrisk.security.infrastructure.apikey.MerchantApiKeyRepository;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MerchantApiKeyAuthenticatorTest {

    private static final Instant NOW = Instant.parse("2026-06-05T12:00:00Z");

    private final MerchantApiKeyRepository repository = mock(MerchantApiKeyRepository.class);
    private final MerchantApiKeyVerifier verifier = mock(MerchantApiKeyVerifier.class);
    private final MerchantApiKeyAuthenticator authenticator = new MerchantApiKeyAuthenticator(repository, verifier);

    @Test
    void resolvesActiveMatchingApiKeyToMerchantActor() {
        when(repository.findByKeyId("key_live")).thenReturn(Mono.just(activeKey("hash_123")));
        when(verifier.matches("key_live", "secret_live", "hash_123")).thenReturn(true);

        StepVerifier.create(authenticator.authenticate(new MerchantApiKeyCredential("key_live", "secret_live")))
                .assertNext(actor -> {
                    assertThat(actor.actorId()).isEqualTo("merchant_123");
                    assertThat(actor.actorType()).isEqualTo(ActorType.MERCHANT);
                    assertThat(actor.roles()).containsExactly(ActorRole.MERCHANT);
                    assertThat(actor.merchantId()).isEqualTo("merchant_123");
                })
                .verifyComplete();
    }

    @Test
    void rejectsUnknownApiKey() {
        when(repository.findByKeyId("unknown")).thenReturn(Mono.empty());

        StepVerifier.create(authenticator.authenticate(new MerchantApiKeyCredential("unknown", "secret_live")))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(InvalidMerchantApiKeyException.class)
                        .hasMessage("Unknown merchant API key"))
                .verify();
    }

    @Test
    void rejectsRevokedApiKey() {
        when(repository.findByKeyId("key_live")).thenReturn(Mono.just(revokedKey()));

        StepVerifier.create(authenticator.authenticate(new MerchantApiKeyCredential("key_live", "secret_live")))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(InvalidMerchantApiKeyException.class)
                        .hasMessage("Merchant API key is not active"))
                .verify();
    }

    @Test
    void rejectsMismatchedSecret() {
        when(repository.findByKeyId("key_live")).thenReturn(Mono.just(activeKey("hash_123")));
        when(verifier.matches("key_live", "wrong_secret", "hash_123")).thenReturn(false);

        StepVerifier.create(authenticator.authenticate(new MerchantApiKeyCredential("key_live", "wrong_secret")))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(InvalidMerchantApiKeyException.class)
                        .hasMessage("Invalid merchant API key secret"))
                .verify();
    }

    private static MerchantApiKeyEntity activeKey(String secretHash) {
        return MerchantApiKeyEntity.builder()
                .id(1L)
                .keyId("key_live")
                .secretHash(secretHash)
                .merchantId("merchant_123")
                .status("ACTIVE")
                .createdAt(NOW)
                .build();
    }

    private static MerchantApiKeyEntity revokedKey() {
        return MerchantApiKeyEntity.builder()
                .id(1L)
                .keyId("key_live")
                .secretHash("hash_123")
                .merchantId("merchant_123")
                .status("REVOKED")
                .createdAt(NOW)
                .build();
    }
}
