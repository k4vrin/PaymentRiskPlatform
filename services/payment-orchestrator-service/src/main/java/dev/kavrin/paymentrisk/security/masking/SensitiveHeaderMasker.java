package dev.kavrin.paymentrisk.security.masking;

import lombok.experimental.UtilityClass;
import org.springframework.http.HttpHeaders;

import java.util.List;
import java.util.Map;

/**
 * Masks sensitive HTTP headers before they are logged or included in diagnostics.
 */
@UtilityClass
public class SensitiveHeaderMasker {

    public static HttpHeaders mask(HttpHeaders headers) {
        var masked = new HttpHeaders();

        headers.forEach((name, values) -> masked.put(name, maskHeaderValues(name, values)));

        return masked;
    }

    public static Map<String, List<String>> mask(Map<String, List<String>> headers) {
        return headers.entrySet()
                .stream()
                .collect(
                        java.util.stream.Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> maskHeaderValues(entry.getKey(), entry.getValue())
                        )
                );
    }

    private static List<String> maskHeaderValues(String name, List<String> values) {
        if (values == null) {
            return List.of();
        }

        if (isSensitiveHeader(name)) {
            return values.stream()
                    .map(value -> "****")
                    .toList();
        }

        return values;
    }

    private static boolean isSensitiveHeader(String name) {
        return HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)
                || "X-API-Key".equalsIgnoreCase(name)
                || "Proxy-Authorization".equalsIgnoreCase(name)
                || "Cookie".equalsIgnoreCase(name)
                || "Set-Cookie".equalsIgnoreCase(name);
    }
}