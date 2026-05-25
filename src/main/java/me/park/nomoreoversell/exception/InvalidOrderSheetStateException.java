package me.park.nomoreoversell.exception;

public class InvalidOrderSheetStateException extends ConflictException {

    private static final String CODE = "INVALID_ORDER_SHEET_STATE";
    private static final String MESSAGE = "처리할 수 없는 주문서 상태입니다.";

    public InvalidOrderSheetStateException() {
        super(CODE, MESSAGE);
    }
}
