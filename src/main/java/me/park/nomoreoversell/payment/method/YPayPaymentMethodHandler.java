package me.park.nomoreoversell.payment.method;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.park.nomoreoversell.payment.domain.PaymentMethod;
import me.park.nomoreoversell.payment.gateway.PaymentGateway;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class YPayPaymentMethodHandler implements PaymentMethodHandler {

    private final PaymentGateway paymentGateway;

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.Y_PAY;
    }

    @Override
    public PaymentApprovalResult approve(PaymentApprovalRequest request) {
        try {
            var response = paymentGateway.approve(new PaymentGateway.PgApprovalRequest(
                    request.userId(),
                    request.paymentMethod(),
                    request.amount()
            ));
            if (!response.success()) {
                return PaymentApprovalResult.fail(response.reason());
            }
            return PaymentApprovalResult.success(response.transactionKey());
        } catch (RuntimeException exception) {
            log.warn(
                    "Y Pay PG 승인 예외. userId={}, amount={}",
                    request.userId(),
                    request.amount(),
                    exception
            );
            return PaymentApprovalResult.fail("PG_APPROVAL_FAILED");
        }
    }

    @Override
    public PaymentCancelResult cancel(PaymentApprovalRequest request, PaymentApprovalResult approvalResult, String reason) {
        try {
            var response = paymentGateway.cancel(new PaymentGateway.PgCancelRequest(
                    approvalResult.transactionKey(),
                    request.amount(),
                    reason
            ));
            if (!response.success()) {
                return PaymentCancelResult.fail(response.reason());
            }
            return PaymentCancelResult.success(response.cancelKey());
        } catch (RuntimeException exception) {
            log.warn(
                    "Y Pay PG 취소 예외. transactionKey={}, amount={}, reason={}",
                    approvalResult.transactionKey(),
                    request.amount(),
                    reason,
                    exception
            );
            return PaymentCancelResult.fail("PG_CANCEL_FAILED");
        }
    }
}
