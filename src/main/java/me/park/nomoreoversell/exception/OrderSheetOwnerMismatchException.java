package me.park.nomoreoversell.exception;

public class OrderSheetOwnerMismatchException extends ConflictException {

    private static final String CODE = "ORDER_SHEET_OWNER_MISMATCH";
    private static final String MESSAGE = "주문서 사용자 정보가 일치하지 않습니다.";

    public OrderSheetOwnerMismatchException() {
        super(CODE, MESSAGE);
    }
}
