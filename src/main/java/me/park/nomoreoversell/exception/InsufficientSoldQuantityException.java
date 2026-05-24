package me.park.nomoreoversell.exception;

public class InsufficientSoldQuantityException extends ConflictException {

    private static final String CODE = "INSUFFICIENT_SOLD_QUANTITY";
    private static final String MESSAGE = "복구할 판매 수량이 부족합니다.";

    public InsufficientSoldQuantityException() {
        super(CODE, MESSAGE);
    }
}
