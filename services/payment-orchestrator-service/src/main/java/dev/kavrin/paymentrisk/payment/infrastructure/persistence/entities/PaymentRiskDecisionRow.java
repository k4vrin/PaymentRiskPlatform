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
@Table("payment_risk_decisions")
public class PaymentRiskDecisionRow {

    @Id
    @Column("payment_risk_decision_id")
    private String paymentRiskDecisionId;

    @Column("payment_id")
    private String paymentId;

    @Column("decision")
    private String decision;

    @Column("score")
    private int score;

    @Column("reason_codes_json")
    private String reasonCodesJson;

    @Column("rule_version")
    private String ruleVersion;

    @Column("decided_at")
    private Instant decidedAt;

    @Column("created_at")
    private Instant createdAt;
}