package dev.kavrin.paymentrisk.idempotency.infrastructure.persistence;

import dev.kavrin.paymentrisk.idempotency.application.StoredIdempotencyResult;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyScope;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyStatus;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.IdempotencyRecordRow;
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
    void toRowMapsScopeKeyFingerprintStatusResponseAndExpiryFields() {
        StoredIdempotencyResult result = new StoredIdempotencyResult(
                "fingerprint-sha256",
                IdempotencyStatus.COMPLETED,
                200,
                Map.of("paymentId", "pay_test"),
                EXPIRES_AT,
                CREATED_AT,
                UPDATED_AT
        );

        IdempotencyRecordRow row = mapper.toRow(
                "idem_rec_01",
                IdempotencyScope.PAYMENT_AUTHORIZATION,
                IdempotencyKey.of("idem_01HX7QK9JP7E5W5NRZ6T5Q3R1A"),
                result,
                "pay_01HX7R0BYV9Y6CNW3HZ7R8E4P2",
                "{\"paymentId\":\"pay_test\"}"
        );

        assertThat(row.getIdempotencyRecordId()).isEqualTo("idem_rec_01");
        assertThat(row.getScope()).isEqualTo("payment_authorization");
        assertThat(row.getIdempotencyKey()).isEqualTo("idem_01HX7QK9JP7E5W5NRZ6T5Q3R1A");
        assertThat(row.getRequestFingerprint()).isEqualTo("fingerprint-sha256");
        assertThat(row.getPaymentId()).isEqualTo("pay_01HX7R0BYV9Y6CNW3HZ7R8E4P2");
        assertThat(row.getStatus()).isEqualTo("COMPLETED");
        assertThat(row.getResponseStatus()).isEqualTo(200);
        assertThat(row.getResponseBodyJson()).isEqualTo("{\"paymentId\":\"pay_test\"}");
        assertThat(row.getExpiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(row.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(row.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    void toStoredResultMapsDurableRowBackToApplicationResult() {
        IdempotencyRecordRow row = new IdempotencyRecordRow(
                "idem_rec_01",
                "payment_authorization",
                "idem_01HX7QK9JP7E5W5NRZ6T5Q3R1A",
                "fingerprint-sha256",
                "pay_01HX7R0BYV9Y6CNW3HZ7R8E4P2",
                "COMPLETED",
                200,
                "{\"paymentId\":\"pay_test\"}",
                EXPIRES_AT,
                CREATED_AT,
                UPDATED_AT
        );
        Map<String, String> responseSnapshot = Map.of("paymentId", "pay_test");

        StoredIdempotencyResult result = mapper.toStoredResult(row, responseSnapshot);

        assertThat(result.requestFingerprint()).isEqualTo("fingerprint-sha256");
        assertThat(result.status()).isEqualTo(IdempotencyStatus.COMPLETED);
        assertThat(result.responseStatus()).isEqualTo(200);
        assertThat(result.responseSnapshot()).isEqualTo(responseSnapshot);
        assertThat(result.expiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(result.createdAt()).isEqualTo(CREATED_AT);
        assertThat(result.updatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    void mapsScopeAndKeyFromRow() {
        IdempotencyRecordRow row = new IdempotencyRecordRow();
        row.setScope("payment_authorization");
        row.setIdempotencyKey("idem_01HX7QK9JP7E5W5NRZ6T5Q3R1A");

        assertThat(mapper.toScope(row)).isEqualTo(IdempotencyScope.PAYMENT_AUTHORIZATION);
        assertThat(mapper.toKey(row)).isEqualTo(IdempotencyKey.of("idem_01HX7QK9JP7E5W5NRZ6T5Q3R1A"));
    }
}
