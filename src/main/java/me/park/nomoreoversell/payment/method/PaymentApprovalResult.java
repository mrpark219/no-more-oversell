package me.park.nomoreoversell.payment.method;

public record PaymentApprovalResult(
        boolean success,
        String transactionKey,
        String reason
) {
    public static PaymentApprovalResult success(String transactionKey) {
        return new PaymentApprovalResult(true, transactionKey, null);
    }

    public static PaymentApprovalResult fail(String reason) {
        return new PaymentApprovalResult(false, null, reason);
    }
}
