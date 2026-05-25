package me.park.nomoreoversell.exception;

public class OrderSheetNotFoundException extends NotFoundException {

    private static final String CODE = "ORDER_SHEET_NOT_FOUND";
    private static final String MESSAGE = "존재하지 않는 주문서입니다.";

    public OrderSheetNotFoundException() {
        super(CODE, MESSAGE);
    }
}
