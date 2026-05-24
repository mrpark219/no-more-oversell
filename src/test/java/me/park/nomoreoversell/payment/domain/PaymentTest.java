package me.park.nomoreoversell.payment.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    @Test
    @DisplayName("결제 준비 객체를 생성한다")
    void readyCreatesReadyPayment() {
        // given
        var orderSheetId = 1L;
        var paymentToken = "payment-token";

        // when
        var payment = Payment.ready(orderSheetId, paymentToken);

        // then
        assertThat(payment.getOrderSheetId()).isEqualTo(orderSheetId);
        assertThat(payment.getPaymentToken()).isEqualTo(paymentToken);
        assertThat(payment.isReady()).isTrue();
    }

    @Test
    @DisplayName("결제 상세를 추가하면 결제와 상세가 연결된다")
    void addDetailConnectsPaymentAndDetail() {
        // given
        var payment = Payment.ready(1L, "payment-token");
        var detail = PaymentDetail.ready(payment, PaymentMethod.CARD, 10_000L, "tx-key");

        // when
        payment.addDetail(detail);

        // then
        assertThat(payment.getDetails()).containsExactly(detail);
        assertThat(detail.getPayment()).isSameAs(payment);
    }

    @Test
    @DisplayName("다른 결제에 연결된 결제 상세는 추가할 수 없다")
    void addDetailFailsWhenDetailBelongsToAnotherPayment() {
        // given
        var payment = Payment.ready(1L, "payment-token");
        var otherPayment = Payment.ready(2L, "other-payment-token");
        var detail = PaymentDetail.ready(otherPayment, PaymentMethod.CARD, 10_000L, "tx-key");

        // when & then
        assertThatThrownBy(() -> payment.addDetail(detail))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("결제수단으로 결제 상세를 찾는다")
    void findDetailReturnsDetailByPaymentMethod() {
        // given
        var payment = paymentWithCardAndPoint();

        // when
        var result = payment.findDetail(PaymentMethod.POINT);

        // then
        assertThat(result).contains(payment.getDetails().get(1));
    }

    @Test
    @DisplayName("PG 결제 상세와 비PG 결제 상세를 구분한다")
    void detailsAreSeparatedByPgRequirement() {
        // given
        var payment = paymentWithCardAndPoint();

        // when & then
        assertThat(payment.pgDetails()).extracting(PaymentDetail::getPaymentMethod)
                .containsExactly(PaymentMethod.CARD);
        assertThat(payment.nonPgDetails()).extracting(PaymentDetail::getPaymentMethod)
                .containsExactly(PaymentMethod.POINT);
    }

    @Test
    @DisplayName("결제 상세 결제 금액 합계를 계산한다")
    void totalPaymentAmountReturnsSumOfDetailPaymentAmounts() {
        // given
        var payment = paymentWithCardAndPoint();

        // when & then
        assertThat(payment.totalPaymentAmount()).isEqualTo(10_000L);
        assertThat(payment.totalCanceledAmount()).isZero();
        assertThat(payment.totalRemainingAmount()).isEqualTo(10_000L);
    }

    @Test
    @DisplayName("결제 상태를 승인 진행 상태로 변경한다")
    void markApprovingChangesStatusToApproving() {
        // given
        var payment = Payment.ready(1L, "payment-token");

        // when
        payment.markApproving();

        // then
        assertThat(payment.isApproving()).isTrue();
    }

    @Test
    @DisplayName("결제 상태를 승인 완료 상태로 변경한다")
    void markApprovedChangesStatusToApproved() {
        // given
        var payment = Payment.ready(1L, "payment-token");

        // when
        payment.markApproved();

        // then
        assertThat(payment.isApproved()).isTrue();
    }

    @Test
    @DisplayName("특정 결제 상세를 일부 취소하면 취소 금액과 남은 금액을 계산한다")
    void cancelDetailWithPartialAmountUpdatesCanceledAndRemainingAmount() {
        // given
        var payment = paymentWithCardAndPoint();
        payment.markApproved();

        // when
        payment.cancelDetail(PaymentMethod.CARD, 5_000L, "cancel-key", "부분 취소");

        // then
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(payment.totalPaymentAmount()).isEqualTo(10_000L);
        assertThat(payment.totalCanceledAmount()).isEqualTo(5_000L);
        assertThat(payment.totalRemainingAmount()).isEqualTo(5_000L);
        assertThat(payment.getDetails().get(0).getStatus()).isEqualTo(PaymentDetailStatus.PARTIAL_CANCELED);
        assertThat(payment.getDetails().get(0).getCancelKey()).isEqualTo("cancel-key");
        assertThat(payment.getDetails().get(1).getStatus()).isEqualTo(PaymentDetailStatus.READY);
    }

    @Test
    @DisplayName("특정 결제 상세를 전액 취소하면 해당 상세를 취소한다")
    void cancelDetailWithFullAmountCancelsDetail() {
        // given
        var payment = paymentWithCardAndPoint();

        // when
        payment.cancelDetail(PaymentMethod.CARD, 7_000L, "cancel-key", "품절");

        // then
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.READY);
        assertThat(payment.totalPaymentAmount()).isEqualTo(10_000L);
        assertThat(payment.totalCanceledAmount()).isEqualTo(7_000L);
        assertThat(payment.totalRemainingAmount()).isEqualTo(3_000L);
        assertThat(payment.getDetails().get(0).getStatus()).isEqualTo(PaymentDetailStatus.CANCELED);
        assertThat(payment.getDetails().get(1).getStatus()).isEqualTo(PaymentDetailStatus.READY);
    }

    @Test
    @DisplayName("모든 결제 상세가 취소되면 결제를 취소 상태로 변경한다")
    void cancelDetailChangesPaymentStatusToCanceledWhenAllDetailsCanceled() {
        // given
        var payment = paymentWithCardAndPoint();

        // when
        payment.cancelDetail(PaymentMethod.CARD, 7_000L, "card-cancel-key", "품절");
        payment.cancelDetail(PaymentMethod.POINT, 3_000L, "point-cancel-key", "품절");

        // then
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(payment.totalCanceledAmount()).isEqualTo(10_000L);
        assertThat(payment.totalRemainingAmount()).isZero();
        assertThat(payment.getDetails()).extracting(PaymentDetail::getStatus)
                .containsOnly(PaymentDetailStatus.CANCELED);
    }

    @Test
    @DisplayName("결제 상세 취소 금액이 남은 금액보다 크면 실패한다")
    void cancelDetailWithGreaterThanRemainingAmountFails() {
        // given
        var payment = paymentWithCardAndPoint();

        // when & then
        assertThatThrownBy(() -> payment.cancelDetail(PaymentMethod.CARD, 7_001L, "cancel-key", "초과 취소"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("결제를 실패 처리하면 결제 자신의 상태만 실패로 변경한다")
    void markFailedChangesOnlyPaymentStatus() {
        // given
        var payment = paymentWithCardAndPoint();

        // when
        payment.markFailed("승인 실패");

        // then
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getDetails()).extracting(PaymentDetail::getStatus)
                .containsExactly(PaymentDetailStatus.READY, PaymentDetailStatus.READY);
    }

    private Payment paymentWithCardAndPoint() {
        var payment = Payment.ready(1L, "payment-token");
        payment.addDetail(PaymentDetail.ready(payment, PaymentMethod.CARD, 7_000L, "tx-key"));
        payment.addDetail(PaymentDetail.ready(payment, PaymentMethod.POINT, 3_000L, null));
        return payment;
    }
}
