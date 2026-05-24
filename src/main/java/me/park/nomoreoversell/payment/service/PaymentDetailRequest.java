package me.park.nomoreoversell.payment.service;

import me.park.nomoreoversell.payment.domain.PaymentMethod;

public record PaymentDetailRequest(
        PaymentMethod paymentMethod,
        long amount
) {
}
