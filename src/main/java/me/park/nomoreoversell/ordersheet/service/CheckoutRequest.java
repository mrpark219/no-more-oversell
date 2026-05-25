package me.park.nomoreoversell.ordersheet.service;

public record CheckoutRequest(
        Long userId,
        Long stayProductId
) {
}
