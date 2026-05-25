package me.park.nomoreoversell.order.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.park.nomoreoversell.common.web.UserId;
import me.park.nomoreoversell.order.service.CreateOrderResponse;
import me.park.nomoreoversell.order.service.OrderService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public CreateOrderResponse createOrder(
            @UserId Long userId,
            @Valid @RequestBody CreateOrderApiRequest request
    ) {
        return orderService.createOrder(request.toServiceRequest(userId));
    }
}
