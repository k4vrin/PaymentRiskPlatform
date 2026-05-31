package dev.kavrin.paymentrisk.idempotency.infrastructure.persistence;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("idempotency_records")
public class IdempotencyRecordEntity {

    @Id
    @Column("idempotency_record_id")
    private String idempotencyRecordId;

    @Column("scope")
    private String scope;

    @Column("idempotency_key")
    private String idempotencyKey;

    @Column("request_fingerprint")
    private String requestFingerprint;

    @Column("payment_id")
    private String paymentId;

    @Column("status")
    private String status;

    @Column("response_status")
    private Integer responseStatus;

    @Column("response_body_json")
    private String responseBodyJson;

    @Column("expires_at")
    private Instant expiresAt;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;
}
