package me.park.nomoreoversell.payment.service;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import me.park.nomoreoversell.payment.domain.PaymentMethod;

public record PaymentDetailRequest(
        @NotNull
        PaymentMethod paymentMethod,
        @Positive
        long amount
) {
}
