package me.park.nomoreoversell.exception;

public class InvalidPaymentCombinationException extends BusinessException {

    private static final String CODE = "INVALID_PAYMENT_COMBINATION";
    private static final String MESSAGE = "잘못된 결제수단 조합입니다.";

    public InvalidPaymentCombinationException() {
        super(CODE, MESSAGE);
    }

    public InvalidPaymentCombinationException(String message) {
        super(CODE, message);
    }
}
