package me.park.nomoreoversell.payment.gateway;

import me.park.nomoreoversell.payment.domain.PaymentMethod;

public interface PaymentGateway {

    PgApprovalResponse approve(PgApprovalRequest request);

    PgCancelResponse cancel(PgCancelRequest request);

    record PgApprovalRequest(Long userId, PaymentMethod paymentMethod, long amount) {
    }

    record PgApprovalResponse(boolean success, String transactionKey, String reason) {
        public static PgApprovalResponse success(String transactionKey) {
            return new PgApprovalResponse(true, transactionKey, null);
        }

        public static PgApprovalResponse fail(String reason) {
            return new PgApprovalResponse(false, null, reason);
        }
    }

    record PgCancelRequest(String transactionKey, long amount, String reason) {
    }

    record PgCancelResponse(boolean success, String cancelKey, String reason) {
        public static PgCancelResponse success(String cancelKey) {
            return new PgCancelResponse(true, cancelKey, null);
        }

        public static PgCancelResponse fail(String reason) {
            return new PgCancelResponse(false, null, reason);
        }
    }
}
