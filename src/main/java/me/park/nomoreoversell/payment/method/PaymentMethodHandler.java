package me.park.nomoreoversell.payment.method;

import me.park.nomoreoversell.payment.domain.PaymentMethod;

public interface PaymentMethodHandler {

    PaymentMethod getPaymentMethod();

    PaymentApprovalResult approve(PaymentApprovalRequest request);

    PaymentCancelResult cancel(PaymentApprovalRequest request, PaymentApprovalResult approvalResult, String reason);
}
