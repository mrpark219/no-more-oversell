package me.park.nomoreoversell.exception;

public class SoldOutException extends ConflictException {

    private static final String CODE = "SOLD_OUT";
    private static final String MESSAGE = "품절된 상품입니다.";

    public SoldOutException() {
        super(CODE, MESSAGE);
    }
}
