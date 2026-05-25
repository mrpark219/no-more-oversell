package me.park.nomoreoversell.payment.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;

@Slf4j
@Component
public class MockPaymentGateway implements PaymentGateway {

    private static final String TRANSACTION_KEY_PREFIX = "PG-";
    private static final String CANCEL_KEY_PREFIX = "PG-CANCEL-";
    private static final double SUCCESS_RATE = 0.9;

    @Override
    public PgApprovalResponse approve(PgApprovalRequest request) {
        if (isFailed()) {
            log.info(
                    "Mock PG 승인 실패. userId={}, paymentMethod={}, amount={}",
                    request.userId(),
                    request.paymentMethod(),
                    request.amount()
            );
            return PgApprovalResponse.fail("PG_APPROVAL_FAILED");
        }

        var transactionKey = TRANSACTION_KEY_PREFIX + UUID.randomUUID();
        log.info(
                "Mock PG 승인 성공. userId={}, paymentMethod={}, amount={}, transactionKey={}",
                request.userId(),
                request.paymentMethod(),
                request.amount(),
                transactionKey
        );
        return PgApprovalResponse.success(transactionKey);
    }

    @Override
    public PgCancelResponse cancel(PgCancelRequest request) {
        if (isFailed()) {
            log.warn(
                    "Mock PG 취소 실패. transactionKey={}, amount={}, reason={}",
                    request.transactionKey(),
                    request.amount(),
                    request.reason()
            );
            return PgCancelResponse.fail("PG_CANCEL_FAILED");
        }

        var cancelKey = CANCEL_KEY_PREFIX + UUID.randomUUID();
        log.info(
                "Mock PG 취소 성공. transactionKey={}, amount={}, reason={}, cancelKey={}",
                request.transactionKey(),
                request.amount(),
                request.reason(),
                cancelKey
        );
        return PgCancelResponse.success(cancelKey);
    }

    private boolean isFailed() {
        return ThreadLocalRandom.current().nextDouble() > SUCCESS_RATE;
    }
}
