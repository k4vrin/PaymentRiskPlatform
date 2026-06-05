package dev.kavrin.paymentrisk.security.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MerchantApiKeyHasherTest {

    private final MerchantApiKeyHasher hasher = new MerchantApiKeyHasher(properties("test-pepper"));

    @Test
    void hashesSecretWithHmacPrefix() {
        var hash = hasher.hash("key_live", "secret_live");

        assertThat(hash)
                .startsWith("hmac-sha256:")
                .doesNotContain("secret_live");
    }

    @Test
    void producesDeterministicHashForSameCredential() {
        assertThat(hasher.hash("key_live", "secret_live"))
                .isEqualTo(hasher.hash("key_live", "secret_live"));
    }

    @Test
    void usesKeyIdAsHashInput() {
        assertThat(hasher.hash("key_live", "secret_live"))
                .isNotEqualTo(hasher.hash("key_rotated", "secret_live"));
    }

    @Test
    void matchesStoredHashUsingConstantTimeComparison() {
        var storedHash = hasher.hash("key_live", "secret_live");

        assertThat(hasher.matches("key_live", "secret_live", storedHash)).isTrue();
        assertThat(hasher.matches("key_live", "wrong_secret", storedHash)).isFalse();
    }

    @Test
    void supportsSecretRotationByHashingNewSecret() {
        var oldHash = hasher.hash("key_live", "old_secret");
        var rotatedHash = hasher.hash("key_live", "new_secret");

        assertThat(rotatedHash).isNotEqualTo(oldHash);
        assertThat(hasher.matches("key_live", "old_secret", rotatedHash)).isFalse();
        assertThat(hasher.matches("key_live", "new_secret", rotatedHash)).isTrue();
    }

    private static ApiKeyHashingProperties properties(String pepper) {
        var properties = new ApiKeyHashingProperties();
        properties.setPepper(pepper);
        return properties;
    }
}
