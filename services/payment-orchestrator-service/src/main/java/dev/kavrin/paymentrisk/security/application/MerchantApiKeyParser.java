package dev.kavrin.paymentrisk.security.application;

import org.springframework.stereotype.Component;

/**
 * Parses merchant API key header values.
 */
@Component
public class MerchantApiKeyParser {

    public MerchantApiKeyCredential parse(String rawHeader) {
        if (rawHeader == null || rawHeader.isBlank()) {
            throw new InvalidMerchantApiKeyException("Missing merchant API key");
        }

        var parts = rawHeader.trim().split("\\.", 2);

        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new InvalidMerchantApiKeyException("Invalid merchant API key format");
        }

        return new MerchantApiKeyCredential(parts[0], parts[1]);
    }
}