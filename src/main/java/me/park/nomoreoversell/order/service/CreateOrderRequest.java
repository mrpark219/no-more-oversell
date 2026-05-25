package me.park.nomoreoversell.order.service;

import me.park.nomoreoversell.payment.service.PaymentDetailRequest;

import java.util.List;

public record CreateOrderRequest(
        Long userId,
        String orderSheetToken,
        Long stayProductId,
        List<PaymentDetailRequest> paymentDetails
) {
}
