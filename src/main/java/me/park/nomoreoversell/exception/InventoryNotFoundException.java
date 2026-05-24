package me.park.nomoreoversell.exception;

public class InventoryNotFoundException extends NotFoundException {

    private static final String CODE = "INVENTORY_NOT_FOUND";
    private static final String MESSAGE = "존재하지 않는 재고입니다.";

    public InventoryNotFoundException() {
        super(CODE, MESSAGE);
    }
}
