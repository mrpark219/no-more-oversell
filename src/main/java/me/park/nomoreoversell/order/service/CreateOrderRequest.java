package me.park.nomoreoversell.order.service;

import java.util.List;

public record CreateOrderRequest(
        Long userId,
        String orderSheetToken,
        Long stayProductId,
        List<CreateOrderPaymentRequest> paymentDetails
) {
}
