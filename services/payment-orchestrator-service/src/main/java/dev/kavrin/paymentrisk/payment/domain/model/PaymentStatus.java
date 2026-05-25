package dev.kavrin.paymentrisk.payment.domain.model;

public enum PaymentStatus {
    RECEIVED,
    RISK_PENDING,
    RISK_APPROVED,
    AUTHORIZED,
    DECLINED,
    REVERSED,
    FAILED
}
