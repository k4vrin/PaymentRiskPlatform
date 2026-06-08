package dev.kavrin.paymentrisk.security.masking;

import lombok.experimental.UtilityClass;

import java.util.regex.Pattern;

/**
 * Central masking helper for values that must never appear in logs,
 * error messages, audit previews, or operational diagnostics.
 *
 * <p>Important: masking is not encryption. Masking only reduces accidental
 * exposure in logs. Secrets must still be hashed/encrypted at rest where needed.</p>
 */
@UtilityClass
public class SensitiveDataMasker {

    private static final String MASK = "****";

    private static final Pattern BEARER_TOKEN_PATTERN =
            Pattern.compile("(?i)Bearer\\s+[A-Za-z0-9._~+/=-]+");

    private static final Pattern API_KEY_HEADER_PATTERN =
            Pattern.compile("(?i)(X-API-Key\\s*[:=]\\s*)([^,\\s]+)");

    private static final Pattern AUTHORIZATION_HEADER_PATTERN =
            Pattern.compile("(?i)(Authorization\\s*[:=]\\s*)(Bearer\\s+)?([^,\\s]+)");

    private static final Pattern JSON_SENSITIVE_FIELD_PATTERN =
            Pattern.compile(
                    "(?i)(\"(?:paymentMethodToken|deviceFingerprint|apiKey|accessToken|refreshToken|token|authorization)\"\\s*:\\s*\")([^\"]+)(\")"
            );

    private static final Pattern TEXT_SENSITIVE_FIELD_PATTERN =
            Pattern.compile(
                    "(?i)(\\b(?:paymentMethodToken|deviceFingerprint|apiKey|accessToken|refreshToken|token|authorization)\\s*[=:]\\s*)([^,\\s}]+)"
            );

    public static String maskPaymentMethodToken(String value) {
        return keepLast(value, 4);
    }

    public static String maskDeviceFingerprint(String value) {
        return keepLast(value, 6);
    }

    public static String maskApiKey(String value) {
        return keepLast(value, 4);
    }

    public static String maskAuthorizationHeader(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        if (value.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
            return "Bearer " + MASK;
        }

        return MASK;
    }

    /**
     * Best-effort masking for larger log messages.
     *
     * <p>This is useful as a safety net, but prefer masking individual fields
     * before building the log message.</p>
     */
    public static String maskFreeText(String message) {
        if (message == null || message.isBlank()) {
            return message;
        }

        var masked = BEARER_TOKEN_PATTERN.matcher(message)
                .replaceAll("Bearer " + MASK);

        masked = API_KEY_HEADER_PATTERN.matcher(masked)
                .replaceAll("$1" + MASK);

        masked = AUTHORIZATION_HEADER_PATTERN.matcher(masked)
                .replaceAll("$1" + MASK);

        masked = JSON_SENSITIVE_FIELD_PATTERN.matcher(masked)
                .replaceAll("$1" + MASK + "$3");

        masked = TEXT_SENSITIVE_FIELD_PATTERN.matcher(masked)
                .replaceAll("$1" + MASK);

        return masked;
    }

    private static String keepLast(String value, int visibleSuffixLength) {
        if (value == null || value.isBlank()) {
            return value;
        }

        if (value.length() <= visibleSuffixLength) {
            return MASK;
        }

        var suffix = value.substring(value.length() - visibleSuffixLength);
        return MASK + suffix;
    }
}
