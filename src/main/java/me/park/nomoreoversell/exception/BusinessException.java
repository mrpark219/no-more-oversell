package me.park.nomoreoversell.exception;

public class BusinessException extends BadRequestException {

    public BusinessException(String message) {
        super("BUSINESS_ERROR", message);
    }

    public BusinessException(String code, String message) {
        super(code, message);
    }

    public BusinessException(String message, Throwable cause) {
        super("BUSINESS_ERROR", message, cause);
    }
}
