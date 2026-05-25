package me.park.nomoreoversell.common;

public record ApiErrorResponse(
        String code,
        String message
) {
}
