package dev.kavrin.paymentrisk.idempotency.infrastructure.persistence;

import dev.kavrin.paymentrisk.idempotency.application.StoredIdempotencyResult;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyScope;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class IdempotencyRecordMapperTest {

    private static final Instant CREATED_AT = Instant.parse("2026-05-26T09:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-05-26T09:01:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-05-27T09:00:00Z");

    private final IdempotencyRecordMapper mapper = new IdempotencyRecordMapper();

    @Test
    void toEntityMapsScopeKeyFingerprintStatusResponseAndExpiryFields() {
        StoredIdempotencyResult result = new StoredIdempotencyResult(
                "fingerprint-sha256",
                IdempotencyStatus.COMPLETED,
                200,
                Map.of("paymentId", "pay_test"),
                EXPIRES_AT,
                CREATED_AT,
                UPDATED_AT
        );

        IdempotencyRecordEntity entity = mapper.toEntity(
                "idem_rec_01",
                IdempotencyScope.PAYMENT_AUTHORIZATION,
                IdempotencyKey.of("idem_01HX7QK9JP7E5W5NRZ6T5Q3R1A"),
                result,
                "pay_01HX7R0BYV9Y6CNW3HZ7R8E4P2",
                "{\"paymentId\":\"pay_test\"}"
        );

        assertThat(entity.getIdempotencyRecordId()).isEqualTo("idem_rec_01");
        assertThat(entity.getScope()).isEqualTo("payment_authorization");
        assertThat(entity.getIdempotencyKey()).isEqualTo("idem_01HX7QK9JP7E5W5NRZ6T5Q3R1A");
        assertThat(entity.getRequestFingerprint()).isEqualTo("fingerprint-sha256");
        assertThat(entity.getPaymentId()).isEqualTo("pay_01HX7R0BYV9Y6CNW3HZ7R8E4P2");
        assertThat(entity.getStatus()).isEqualTo("COMPLETED");
        assertThat(entity.getResponseStatus()).isEqualTo(200);
        assertThat(entity.getResponseBodyJson()).isEqualTo("{\"paymentId\":\"pay_test\"}");
        assertThat(entity.getExpiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(entity.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(entity.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    void toStoredResultMapsDurableEntityBackToApplicationResult() {
        IdempotencyRecordEntity entity = IdempotencyRecordEntity.builder()
                .idempotencyRecordId("idem_rec_01")
                .scope("payment_authorization")
                .idempotencyKey("idem_01HX7QK9JP7E5W5NRZ6T5Q3R1A")
                .requestFingerprint("fingerprint-sha256")
                .paymentId("pay_01HX7R0BYV9Y6CNW3HZ7R8E4P2")
                .status("COMPLETED")
                .responseStatus(200)
                .responseBodyJson("{\"paymentId\":\"pay_test\"}")
                .expiresAt(EXPIRES_AT)
                .createdAt(CREATED_AT)
                .updatedAt(UPDATED_AT)
                .build();
        Map<String, String> responseSnapshot = Map.of("paymentId", "pay_test");

        StoredIdempotencyResult result = mapper.toStoredResult(entity, responseSnapshot);

        assertThat(result.requestFingerprint()).isEqualTo("fingerprint-sha256");
        assertThat(result.status()).isEqualTo(IdempotencyStatus.COMPLETED);
        assertThat(result.responseStatus()).isEqualTo(200);
        assertThat(result.responseSnapshot()).isEqualTo(responseSnapshot);
        assertThat(result.expiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(result.createdAt()).isEqualTo(CREATED_AT);
        assertThat(result.updatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    void mapsScopeAndKeyFromEntity() {
        IdempotencyRecordEntity entity = IdempotencyRecordEntity.builder()
                .scope("payment_authorization")
                .idempotencyKey("idem_01HX7QK9JP7E5W5NRZ6T5Q3R1A")
                .build();

        assertThat(mapper.toScope(entity)).isEqualTo(IdempotencyScope.PAYMENT_AUTHORIZATION);
        assertThat(mapper.toKey(entity)).isEqualTo(IdempotencyKey.of("idem_01HX7QK9JP7E5W5NRZ6T5Q3R1A"));
    }
}
