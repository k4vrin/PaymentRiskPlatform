package dev.kavrin.paymentrisk.security.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Verifies merchant API key secrets against stored hashes.
 */
@Component
@RequiredArgsConstructor
public class MerchantApiKeyVerifier {

    private final MerchantApiKeyHasher hasher;

    public boolean matches(String keyId, String rawSecret, String storedHash) {
        return hasher.matches(keyId, rawSecret, storedHash);
    }
}