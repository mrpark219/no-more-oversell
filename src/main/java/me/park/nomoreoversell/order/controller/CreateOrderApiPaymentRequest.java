package me.park.nomoreoversell.order.controller;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import me.park.nomoreoversell.order.service.CreateOrderPaymentRequest;
import me.park.nomoreoversell.payment.domain.PaymentMethod;

public record CreateOrderApiPaymentRequest(
        @NotNull
        PaymentMethod paymentMethod,
        @Positive
        long amount
) {

    CreateOrderPaymentRequest toServiceRequest() {
        return new CreateOrderPaymentRequest(paymentMethod, amount);
    }
}
