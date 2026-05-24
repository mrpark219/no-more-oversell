package me.park.nomoreoversell.exception;

public class PointNotFoundException extends NotFoundException {

    private static final String CODE = "POINT_NOT_FOUND";
    private static final String MESSAGE = "존재하지 않는 포인트입니다.";

    public PointNotFoundException() {
        super(CODE, MESSAGE);
    }
}
