package me.park.nomoreoversell.payment.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentMethod {
    CARD(true, 0),
    Y_PAY(true, 0),
    POINT(false, 1);

    private final boolean pgNeeded;
    private final int payOrder;
}
