package me.park.nomoreoversell.exception;

public class PurchaseLimitExceededException extends ConflictException {

    private static final String CODE = "PURCHASE_LIMIT_EXCEEDED";
    private static final String MESSAGE = "인당 구매 제한을 초과했습니다.";

    public PurchaseLimitExceededException() {
        super(CODE, MESSAGE);
    }
}
