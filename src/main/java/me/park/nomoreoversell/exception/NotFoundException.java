package me.park.nomoreoversell.exception;

public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super("NOT_FOUND", message);
    }

    public NotFoundException(String code, String message) {
        super(code, message);
    }
}
