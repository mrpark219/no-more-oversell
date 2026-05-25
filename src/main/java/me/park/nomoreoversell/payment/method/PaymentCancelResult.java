package me.park.nomoreoversell.payment.method;

public record PaymentCancelResult(
        boolean success,
        String cancelKey,
        String reason
) {
    public static PaymentCancelResult success(String cancelKey) {
        return new PaymentCancelResult(true, cancelKey, null);
    }

    public static PaymentCancelResult fail(String reason) {
        return new PaymentCancelResult(false, null, reason);
    }
}
