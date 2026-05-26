package dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table("payment_authorizations")
public class PaymentAuthorizationRow {

    @Id
    @Column("payment_authorization_id")
    private String paymentAuthorizationId;

    @Column("payment_id")
    private String paymentId;

    @Column("status")
    private String status;

    @Column("authorization_code")
    private String authorizationCode;

    @Column("failure_reason")
    private String failureReason;

    @Column("requested_at")
    private Instant requestedAt;

    @Column("risk_pending_at")
    private Instant riskPendingAt;

    @Column("authorized_at")
    private Instant authorizedAt;

    @Column("declined_at")
    private Instant declinedAt;

    @Column("failed_at")
    private Instant failedAt;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;
}