package me.park.nomoreoversell.ordersheet.domain;

public enum OrderSheetStatus {
    CREATED,
    READY,
    APPROVING,
    CONFIRMED,
    SOLD_OUT,
    PAYMENT_FAILED,
    EXPIRED
}
