package me.park.nomoreoversell.payment.domain;

public enum PaymentStatus {
    READY,
    APPROVING,
    APPROVED,
    CANCELED,
    CANCEL_FAILED,
    FAILED
}
