package dev.kavrin.paymentrisk.security.masking;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveHeaderMaskerTest {

    @Test
    void masksAuthorizationHeader() {
        var headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer ey.fake.jwt");

        var masked = SensitiveHeaderMasker.mask(headers);

        assertThat(masked.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("****");
    }

    @Test
    void masksApiKeyHeader() {
        var headers = new HttpHeaders();
        headers.add("X-API-Key", "payrisk_live_abcdef1234");

        var masked = SensitiveHeaderMasker.mask(headers);

        assertThat(masked.getFirst("X-API-Key")).isEqualTo("****");
    }

    @Test
    void keepsNonSensitiveHeaders() {
        var headers = new HttpHeaders();
        headers.add("X-Correlation-Id", "corr-123");

        var masked = SensitiveHeaderMasker.mask(headers);

        assertThat(masked.getFirst("X-Correlation-Id")).isEqualTo("corr-123");
    }

    @Test
    void masksCookieHeaders() {
        var headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "SESSION=secret");
        headers.add(HttpHeaders.SET_COOKIE, "SESSION=secret");

        var masked = SensitiveHeaderMasker.mask(headers);

        assertThat(masked.getFirst(HttpHeaders.COOKIE)).isEqualTo("****");
        assertThat(masked.getFirst(HttpHeaders.SET_COOKIE)).isEqualTo("****");
    }
}
