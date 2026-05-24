package me.park.nomoreoversell.exception;

public class InvalidInventoryQuantityException extends BusinessException {

    private static final String CODE = "INVALID_INVENTORY_QUANTITY";
    private static final String MESSAGE = "재고 수량은 1개 이상이어야 합니다.";

    public InvalidInventoryQuantityException() {
        super(CODE, MESSAGE);
    }
}
