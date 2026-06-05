package dev.kavrin.paymentrisk.security.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Hashes merchant API key secrets before storage and verification.
 *
 * <p>API keys should be high-entropy random secrets. For that reason, a keyed
 * HMAC is a good fit: it is deterministic for lookup verification and prevents
 * database-only leaks from exposing usable API keys.</p>
 */
@Component
@RequiredArgsConstructor
public class MerchantApiKeyHasher {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String HASH_PREFIX = "hmac-sha256:";

    private final ApiKeyHashingProperties properties;

    public String hash(String keyId, String rawSecret) {
        if (keyId == null || keyId.isBlank()) {
            throw new IllegalArgumentException("keyId is required");
        }

        if (rawSecret == null || rawSecret.isBlank()) {
            throw new IllegalArgumentException("rawSecret is required");
        }

        try {
            var mac = Mac.getInstance(HMAC_ALGORITHM);
            var secretKey = new SecretKeySpec(
                    properties.getPepper().getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM
            );

            mac.init(secretKey);

            var message = keyId + ":" + rawSecret;
            var hashBytes = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));

            return HASH_PREFIX + HexFormat.of().formatHex(hashBytes);
        } catch (Exception error) {
            throw new IllegalStateException("Failed to hash merchant API key", error);
        }
    }

    public boolean matches(String keyId, String rawSecret, String storedHash) {
        if (storedHash == null || storedHash.isBlank()) {
            return false;
        }

        var candidateHash = hash(keyId, rawSecret);

        return MessageDigest.isEqual(
                candidateHash.getBytes(StandardCharsets.UTF_8),
                storedHash.getBytes(StandardCharsets.UTF_8)
        );
    }
}