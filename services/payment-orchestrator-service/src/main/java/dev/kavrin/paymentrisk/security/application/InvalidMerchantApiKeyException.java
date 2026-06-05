package dev.kavrin.paymentrisk.security.application;

/**
 * Raised when a merchant API key is missing, malformed, unknown, revoked, or invalid.
 */
public class InvalidMerchantApiKeyException extends RuntimeException {

    public InvalidMerchantApiKeyException(String message) {
        super(message);
    }
}