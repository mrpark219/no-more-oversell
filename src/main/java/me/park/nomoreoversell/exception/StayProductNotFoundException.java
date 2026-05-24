package me.park.nomoreoversell.exception;

public class StayProductNotFoundException extends NotFoundException {

    private static final String CODE = "STAY_PRODUCT_NOT_FOUND";
    private static final String MESSAGE = "존재하지 않는 숙소 상품입니다.";

    public StayProductNotFoundException() {
        super(CODE, MESSAGE);
    }
}
