package dev.kavrin.paymentrisk.security.masking;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataMaskerTest {

    @Test
    void masksPaymentMethodTokenKeepingLastFourCharacters() {
        var masked = SensitiveDataMasker.maskPaymentMethodToken("tok_1234567890");

        assertThat(masked).isEqualTo("****7890");
    }

    @Test
    void masksShortPaymentMethodTokenCompletely() {
        var masked = SensitiveDataMasker.maskPaymentMethodToken("123");

        assertThat(masked).isEqualTo("****");
    }

    @Test
    void masksDeviceFingerprintKeepingLastSixCharacters() {
        var masked = SensitiveDataMasker.maskDeviceFingerprint("device-fingerprint-abcdef");

        assertThat(masked).isEqualTo("****abcdef");
    }

    @Test
    void masksApiKeyKeepingLastFourCharacters() {
        var masked = SensitiveDataMasker.maskApiKey("payrisk_live_abcdef1234");

        assertThat(masked).isEqualTo("****1234");
    }

    @Test
    void masksAuthorizationHeader() {
        var masked = SensitiveDataMasker.maskAuthorizationHeader("Bearer ey.fake.jwt");

        assertThat(masked).isEqualTo("Bearer ****");
    }

    @Test
    void masksNonBearerAuthorizationHeader() {
        var masked = SensitiveDataMasker.maskAuthorizationHeader("Basic dXNlcjpwYXNz");

        assertThat(masked).isEqualTo("****");
    }

    @Test
    void masksBearerTokenInsideFreeText() {
        var masked = SensitiveDataMasker.maskFreeText("Authorization: Bearer ey.fake.jwt");

        assertThat(masked).isEqualTo("Authorization: ****");
    }

    @Test
    void masksApiKeyInsideFreeText() {
        var masked = SensitiveDataMasker.maskFreeText("X-API-Key: payrisk_live_abcdef1234");

        assertThat(masked).isEqualTo("X-API-Key: ****");
    }

    @Test
    void masksSensitiveJsonFieldsInsideFreeText() {
        var masked = SensitiveDataMasker.maskFreeText("""
                {"paymentMethodToken":"tok_1234567890","deviceFingerprint":"dfp_secret_abcdef","apiKey":"key.secret"}
                """);

        assertThat(masked)
                .doesNotContain("tok_1234567890")
                .doesNotContain("dfp_secret_abcdef")
                .doesNotContain("key.secret")
                .contains("\"paymentMethodToken\":\"****\"")
                .contains("\"deviceFingerprint\":\"****\"")
                .contains("\"apiKey\":\"****\"");
    }

    @Test
    void masksSensitiveKeyValueFieldsInsideFreeText() {
        var masked = SensitiveDataMasker.maskFreeText(
                "paymentMethodToken=tok_1234567890 deviceFingerprint=dfp_secret_abcdef token=jwt.secret"
        );

        assertThat(masked)
                .doesNotContain("tok_1234567890")
                .doesNotContain("dfp_secret_abcdef")
                .doesNotContain("jwt.secret")
                .contains("paymentMethodToken=****")
                .contains("deviceFingerprint=****")
                .contains("token=****");
    }
}
