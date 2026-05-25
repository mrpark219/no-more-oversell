package me.park.nomoreoversell.order.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import me.park.nomoreoversell.order.service.CreateOrderRequest;

import java.util.List;

public record CreateOrderApiRequest(
        @NotBlank
        String orderSheetToken,
        @NotNull
        @Positive
        Long stayProductId,
        @NotEmpty
        @Valid
        List<CreateOrderApiPaymentRequest> paymentDetails
) {

    CreateOrderRequest toServiceRequest(Long userId) {
        return new CreateOrderRequest(
                userId,
                orderSheetToken,
                stayProductId,
                paymentDetails.stream()
                        .map(CreateOrderApiPaymentRequest::toServiceRequest)
                        .toList()
        );
    }
}
