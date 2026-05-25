package me.park.nomoreoversell.payment.method;

import lombok.RequiredArgsConstructor;
import me.park.nomoreoversell.payment.domain.PaymentMethod;
import me.park.nomoreoversell.payment.gateway.PaymentGateway;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CardPaymentMethodHandler implements PaymentMethodHandler {

    private final PaymentGateway paymentGateway;

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.CARD;
    }

    @Override
    public PaymentApprovalResult approve(PaymentApprovalRequest request) {
        var response = paymentGateway.approve(new PaymentGateway.PgApprovalRequest(
                request.userId(),
                request.paymentMethod(),
                request.amount()
        ));
        if (!response.success()) {
            return PaymentApprovalResult.fail(response.reason());
        }
        return PaymentApprovalResult.success(response.transactionKey());
    }

    @Override
    public PaymentCancelResult cancel(PaymentApprovalRequest request, PaymentApprovalResult approvalResult, String reason) {
        var response = paymentGateway.cancel(new PaymentGateway.PgCancelRequest(
                approvalResult.transactionKey(),
                request.amount(),
                reason
        ));
        if (!response.success()) {
            return PaymentCancelResult.fail(response.reason());
        }
        return PaymentCancelResult.success(response.cancelKey());
    }
}
