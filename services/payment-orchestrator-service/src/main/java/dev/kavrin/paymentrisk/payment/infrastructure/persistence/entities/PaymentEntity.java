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
@Table("payments")
public class PaymentEntity {

    @Id
    @Column("payment_id")
    private String paymentId;

    @Column("merchant_id")
    private String merchantId;

    @Column("customer_id")
    private String customerId;

    @Column("amount_minor")
    private long amountMinor;

    @Column("currency")
    private String currency;

    @Column("payment_method_token_hash")
    private String paymentMethodTokenHash;

    @Column("payment_method_token_last4")
    private String paymentMethodTokenLast4;

    @Column("device_fingerprint_hash")
    private String deviceFingerprintHash;

    @Column("external_reference")
    private String externalReference;

    @Column("idempotency_key")
    private String idempotencyKey;

    @Column("status")
    private String status;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

}
