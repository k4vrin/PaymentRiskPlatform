package dev.kavrin.paymentrisk.idempotency.infrastructure.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisIdempotencySnapshotSerializer {

    private final ObjectMapper objectMapper;

    public String serialize(CachedIdempotencySnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Failed to serialize Redis idempotency snapshot",
                    exception
            );
        }
    }

    public CachedIdempotencySnapshot deserialize(String json) {
        try {
            return objectMapper.readValue(json, CachedIdempotencySnapshot.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Failed to deserialize Redis idempotency snapshot",
                    exception
            );
        }
    }
}
