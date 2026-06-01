package dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities;

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
@Table("payment_reversals")
public class PaymentReversalEntity {

    @Id
    @Column("payment_reversal_id")
    private String paymentReversalId;

    @Column("payment_id")
    private String paymentId;

    @Column("merchant_id")
    private String merchantId;

    @Column("customer_id")
    private String customerId;

    @Column("idempotency_key")
    private String idempotencyKey;

    @Column("reason")
    private String reason;

    @Column("status")
    private String status;

    @Column("requested_at")
    private Instant requestedAt;

    @Column("reversed_at")
    private Instant reversedAt;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;
}
