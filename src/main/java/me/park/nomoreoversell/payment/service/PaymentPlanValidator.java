package me.park.nomoreoversell.payment.service;

import me.park.nomoreoversell.exception.InvalidPaymentCombinationException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentPlanValidator {

    public void validate(long salePrice, List<PaymentDetailRequest> paymentDetailRequests) {
        if (salePrice <= 0) {
            throw new InvalidPaymentCombinationException();
        }
        PaymentCompositionPolicy.validate(paymentDetailRequests);

        var totalPaid = 0L;
        for (var paymentDetailRequest : paymentDetailRequests) {
            if (paymentDetailRequest.amount() <= 0) {
                throw new InvalidPaymentCombinationException();
            }
            totalPaid += paymentDetailRequest.amount();
        }
        if (totalPaid != salePrice) {
            throw new InvalidPaymentCombinationException();
        }
    }
}
