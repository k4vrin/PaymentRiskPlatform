package dev.kavrin.paymentrisk.security.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MerchantApiKeyParserTest {

    private final MerchantApiKeyParser parser = new MerchantApiKeyParser();

    @Test
    void parsesKeyIdAndSecret() {
        var credential = parser.parse("key_live_123.secret_456");

        assertThat(credential.keyId()).isEqualTo("key_live_123");
        assertThat(credential.secret()).isEqualTo("secret_456");
    }

    @Test
    void rejectsMissingHeader() {
        assertThatThrownBy(() -> parser.parse(null))
                .isInstanceOf(InvalidMerchantApiKeyException.class)
                .hasMessage("Missing merchant API key");
    }

    @Test
    void rejectsMalformedHeader() {
        assertThatThrownBy(() -> parser.parse("key-without-secret"))
                .isInstanceOf(InvalidMerchantApiKeyException.class)
                .hasMessage("Invalid merchant API key format");
    }
}
