package me.park.nomoreoversell.exception;

public class ConflictException extends ApiException {

    public ConflictException(String code, String message) {
        super(code, message);
    }
}
