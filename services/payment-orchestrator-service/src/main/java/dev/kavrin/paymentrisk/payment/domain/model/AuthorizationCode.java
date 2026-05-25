package dev.kavrin.paymentrisk.payment.domain.model;

import java.security.SecureRandom;

public record AuthorizationCode(String value) {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    public AuthorizationCode {
        value = RequiredText.require(value, "authorizationCode", 40).toUpperCase();
    }

    public static AuthorizationCode of(String value) {
        return new AuthorizationCode(value);
    }

    public static AuthorizationCode generate() {
        char[] code = new char[12];
        for (int i = 0; i < code.length; i++) {
            code[i] = ALPHABET[RANDOM.nextInt(ALPHABET.length)];
        }
        return new AuthorizationCode("AUTH-" + new String(code));
    }
}
