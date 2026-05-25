package me.park.nomoreoversell.payment.service;

import me.park.nomoreoversell.exception.InvalidPaymentCombinationException;

import java.util.List;

public final class PaymentCompositionPolicy {

    private PaymentCompositionPolicy() {
    }

    public static void validate(List<PaymentDetailRequest> paymentDetailRequests) {
        if (paymentDetailRequests.isEmpty()) {
            throw new InvalidPaymentCombinationException("결제수단이 비었습니다.");
        }

        var pgCount = paymentDetailRequests.stream()
                .filter(detail -> detail.paymentMethod().isPgNeeded())
                .count();
        if (pgCount > 1) {
            throw new InvalidPaymentCombinationException("PG 결제가 필요한 결제수단은 1개까지만 사용할 수 있습니다.");
        }

        var distinctCount = paymentDetailRequests.stream()
                .map(PaymentDetailRequest::paymentMethod)
                .distinct()
                .count();
        if (distinctCount != paymentDetailRequests.size()) {
            throw new InvalidPaymentCombinationException("동일한 결제수단을 중복으로 사용할 수 없습니다.");
        }
    }
}
