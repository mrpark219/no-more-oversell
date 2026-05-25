package me.park.nomoreoversell.payment.method;

import me.park.nomoreoversell.payment.domain.PaymentMethod;
import me.park.nomoreoversell.payment.gateway.PaymentGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentMethodHandlerTest {

    @Test
    @DisplayName("카드 PG 승인 중 예외가 발생하면 승인 실패 결과를 반환한다")
    void cardApproveReturnsFailureWhenPaymentGatewayThrowsException() {
        // given
        var handler = new CardPaymentMethodHandler(new ThrowingPaymentGateway());

        // when
        var result = handler.approve(new PaymentApprovalRequest(1L, PaymentMethod.CARD, 10_000L));

        // then
        assertThat(result.success()).isFalse();
        assertThat(result.reason()).isEqualTo("PG_APPROVAL_FAILED");
    }

    @Test
    @DisplayName("Y Pay PG 취소 중 예외가 발생하면 취소 실패 결과를 반환한다")
    void yPayCancelReturnsFailureWhenPaymentGatewayThrowsException() {
        // given
        var handler = new YPayPaymentMethodHandler(new ThrowingPaymentGateway());
        var request = new PaymentApprovalRequest(1L, PaymentMethod.Y_PAY, 10_000L);
        var approvalResult = PaymentApprovalResult.success("PG-1");

        // when
        var result = handler.cancel(request, approvalResult, "ORDER_CONFIRM_FAILED");

        // then
        assertThat(result.success()).isFalse();
        assertThat(result.reason()).isEqualTo("PG_CANCEL_FAILED");
    }

    private static class ThrowingPaymentGateway implements PaymentGateway {

        @Override
        public PgApprovalResponse approve(PgApprovalRequest request) {
            throw new RuntimeException("PG approval timeout");
        }

        @Override
        public PgCancelResponse cancel(PgCancelRequest request) {
            throw new RuntimeException("PG cancel timeout");
        }
    }
}
