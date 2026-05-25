package me.park.nomoreoversell.payment.method;

import lombok.RequiredArgsConstructor;
import me.park.nomoreoversell.exception.InvalidPaymentCombinationException;
import me.park.nomoreoversell.payment.domain.PaymentMethod;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PaymentMethodHandlerFinder {

    private final List<PaymentMethodHandler> paymentMethodHandlers;

    public PaymentMethodHandler get(PaymentMethod paymentMethod) {
        return paymentMethodHandlers.stream()
                .filter(handler -> handler.getPaymentMethod() == paymentMethod)
                .findFirst()
                .orElseThrow(() -> new InvalidPaymentCombinationException(
                        "지원하지 않는 결제수단입니다. paymentMethod=" + paymentMethod
                ));
    }
}
