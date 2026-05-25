package me.park.nomoreoversell.payment.method;

import me.park.nomoreoversell.payment.domain.PaymentMethod;

public record PaymentApprovalRequest(
        Long userId,
        PaymentMethod paymentMethod,
        long amount
) {
}
