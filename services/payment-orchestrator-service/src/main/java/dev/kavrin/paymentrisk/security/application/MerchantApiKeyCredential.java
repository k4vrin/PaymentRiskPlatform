package dev.kavrin.paymentrisk.security.application;

/**
 * Parsed merchant API key credential from the HTTP request.
 *
 * <p>The key ID is used for lookup. The secret is verified against the stored
 * hash. This avoids scanning all keys during authentication.</p>
 */
public record MerchantApiKeyCredential(
        String keyId,
        String secret
) {
}