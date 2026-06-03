package dev.kavrin.paymentrisk.payment.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import dev.kavrin.paymentrisk.payment.application.command.AuthorizePaymentResult;
import dev.kavrin.paymentrisk.payment.application.command.ReversePaymentResult;
import org.springframework.stereotype.Component;

@Component
public class AuthorizePaymentResultSnapshotSerializer {

    private final ObjectMapper objectMapper;

    public AuthorizePaymentResultSnapshotSerializer() {
        this(JsonMapper.builder()
                .findAndAddModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build());
    }

    public AuthorizePaymentResultSnapshotSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serialize(Object snapshot) {
        if (!isSupportedSnapshot(snapshot)) {
            throw unsupportedSnapshotType(snapshot);
        }

        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Failed to serialize idempotency response snapshot",
                    exception
            );
        }
    }

    public <T> T deserialize(String json, Class<T> snapshotType) {
        if (!isSupportedSnapshotClass(snapshotType)) {
            throw unsupportedSnapshotClass(snapshotType);
        }

        try {
            return objectMapper.readValue(json, snapshotType);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Failed to deserialize idempotency response snapshot",
                    exception
            );
        }
    }

    private static boolean isSupportedSnapshot(Object snapshot) {
        return snapshot instanceof AuthorizePaymentResult
                || snapshot instanceof ReversePaymentResult;
    }

    private static boolean isSupportedSnapshotClass(Class<?> snapshotType) {
        return AuthorizePaymentResult.class.equals(snapshotType)
                || ReversePaymentResult.class.equals(snapshotType);
    }

    private static UnsupportedOperationException unsupportedSnapshotType(Object snapshot) {
        String typeName = snapshot == null ? "null" : snapshot.getClass().getName();
        return new UnsupportedOperationException(
                "Unsupported idempotency response snapshot type: " + typeName
        );
    }

    private static UnsupportedOperationException unsupportedSnapshotClass(Class<?> snapshotType) {
        String typeName = snapshotType == null ? "null" : snapshotType.getName();
        return new UnsupportedOperationException(
                "Unsupported idempotency response snapshot class: " + typeName
        );
    }
}
