package me.park.nomoreoversell.ordersheet.domain;

public enum OrderSheetFailureReason {
    PAYMENT_FAILED,
    SOLD_OUT,
    PURCHASE_LIMIT_EXCEEDED,
    ORDER_CONFIRM_FAILED
}
