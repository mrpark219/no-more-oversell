package me.park.nomoreoversell.exception;

public class StayProductNotOpenException extends ConflictException {

    private static final String CODE = "PRODUCT_NOT_OPEN";
    private static final String MESSAGE = "아직 오픈되지 않은 숙소 상품입니다.";

    public StayProductNotOpenException() {
        super(CODE, MESSAGE);
    }
}
