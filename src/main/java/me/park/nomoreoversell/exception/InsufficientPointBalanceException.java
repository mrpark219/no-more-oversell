package me.park.nomoreoversell.exception;

public class InsufficientPointBalanceException extends BusinessException {

    private static final String CODE = "INSUFFICIENT_POINT_BALANCE";
    private static final String MESSAGE = "포인트 잔액이 부족합니다.";

    public InsufficientPointBalanceException() {
        super(CODE, MESSAGE);
    }
}
