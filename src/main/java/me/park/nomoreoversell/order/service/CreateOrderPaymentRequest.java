package me.park.nomoreoversell.order.service;

import me.park.nomoreoversell.payment.domain.PaymentMethod;

public record CreateOrderPaymentRequest(
        PaymentMethod paymentMethod,
        long amount
) {
}
