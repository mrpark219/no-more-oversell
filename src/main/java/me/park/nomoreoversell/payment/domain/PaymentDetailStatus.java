package me.park.nomoreoversell.payment.domain;

public enum PaymentDetailStatus {
    READY,
    APPROVING,
    APPROVED,
    PARTIAL_CANCELED,
    CANCELED,
    FAILED
}
