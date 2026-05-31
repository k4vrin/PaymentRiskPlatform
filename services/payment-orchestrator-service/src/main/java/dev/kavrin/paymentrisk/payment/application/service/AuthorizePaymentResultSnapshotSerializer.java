package dev.kavrin.paymentrisk.payment.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import dev.kavrin.paymentrisk.payment.application.command.AuthorizePaymentResult;
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
        if (!(snapshot instanceof AuthorizePaymentResult result)) {
            throw unsupportedSnapshotType(snapshot);
        }

        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Failed to serialize AuthorizePaymentResult idempotency snapshot",
                    exception
            );
        }
    }

    public AuthorizePaymentResult deserialize(String json, Class<?> snapshotType) {
        if (!AuthorizePaymentResult.class.equals(snapshotType)) {
            throw unsupportedSnapshotClass(snapshotType);
        }

        try {
            return objectMapper.readValue(json, AuthorizePaymentResult.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Failed to deserialize AuthorizePaymentResult idempotency snapshot",
                    exception
            );
        }
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
