package me.park.nomoreoversell.exception;

public class BadRequestException extends ApiException {

    public BadRequestException(String code, String message) {
        super(code, message);
    }

    public BadRequestException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
