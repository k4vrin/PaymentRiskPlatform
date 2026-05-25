package dev.kavrin.paymentrisk.payment.domain;

public enum PaymentStatus {
    RECEIVED,
    RISK_PENDING,
    RISK_APPROVED,
    AUTHORIZED,
    DECLINED,
    REVERSED,
    FAILED
}
