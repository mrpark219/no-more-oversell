package me.park.nomoreoversell.ordersheet.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import me.park.nomoreoversell.common.web.UserId;
import me.park.nomoreoversell.ordersheet.service.CheckoutRequest;
import me.park.nomoreoversell.ordersheet.service.CheckoutResponse;
import me.park.nomoreoversell.ordersheet.service.OrderSheetService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/checkout")
public class CheckoutController {

    private final OrderSheetService orderSheetService;

    @GetMapping
    public CheckoutResponse checkout(
            @UserId Long userId,
            @RequestParam @Positive Long stayProductId
    ) {
        return orderSheetService.checkout(new CheckoutRequest(userId, stayProductId));
    }
}
