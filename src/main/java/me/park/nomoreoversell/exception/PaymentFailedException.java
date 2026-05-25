package me.park.nomoreoversell.exception;

public class PaymentFailedException extends ConflictException {

    private static final String CODE = "PAYMENT_FAILED";
    private static final String MESSAGE = "결제 승인에 실패했습니다.";

    public PaymentFailedException() {
        super(CODE, MESSAGE);
    }
}
