package dev.kavrin.paymentrisk.ops.domain;

public enum ReplayJobStatus {
    REQUESTED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    REJECTED
}