package dev.kavrin.paymentrisk.settlement.domain;

/**
 * Settlement-facing state derived from payment lifecycle events.
 */
public enum SettlementProjectionStatus {
    SETTLEMENT_READY,
    NOT_SETTLED,
    REVERSED
}