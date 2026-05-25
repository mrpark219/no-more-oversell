package me.park.nomoreoversell.payment.method;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.park.nomoreoversell.payment.domain.PaymentMethod;
import me.park.nomoreoversell.point.service.PointService;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PointPaymentMethodHandler implements PaymentMethodHandler {

    private final PointService pointService;

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.POINT;
    }

    @Override
    public PaymentApprovalResult approve(PaymentApprovalRequest request) {
        if (!pointService.deductIfEnough(request.userId(), request.amount())) {
            return PaymentApprovalResult.fail("INSUFFICIENT_POINT_BALANCE");
        }
        return PaymentApprovalResult.success(transactionKey(request.userId(), request.amount()));
    }

    @Override
    public PaymentCancelResult cancel(PaymentApprovalRequest request, PaymentApprovalResult approvalResult, String reason) {
        pointService.restore(request.userId(), request.amount());
        log.info("포인트 결제 취소 완료. userId={}, amount={}, reason={}", request.userId(), request.amount(), reason);
        return PaymentCancelResult.success(approvalResult.transactionKey());
    }

    private String transactionKey(Long userId, long amount) {
        return "POINT:" + userId + ":" + amount;
    }
}
