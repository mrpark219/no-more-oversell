package me.park.nomoreoversell.exception;

public class OrderInProgressException extends ConflictException {

    private static final String CODE = "ORDER_IN_PROGRESS";
    private static final String MESSAGE = "처리 중인 주문입니다.";

    public OrderInProgressException() {
        super(CODE, MESSAGE);
    }
}
