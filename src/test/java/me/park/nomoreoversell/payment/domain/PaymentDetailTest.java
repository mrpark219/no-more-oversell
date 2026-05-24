package me.park.nomoreoversell.payment.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentDetailTest {

    @Test
    @DisplayName("결제 상세 준비 객체를 생성한다")
    void readyCreatesReadyDetail() {
        // given
        var amount = 10_000L;

        // when
        var detail = PaymentDetail.ready(null, PaymentMethod.CARD, amount, "tx-key");

        // then
        assertThat(detail.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(detail.getPaymentAmount()).isEqualTo(amount);
        assertThat(detail.getCanceledAmount()).isZero();
        assertThat(detail.getRemainingAmount()).isEqualTo(amount);
        assertThat(detail.getExternalTransactionKey()).isEqualTo("tx-key");
        assertThat(detail.getCancelKey()).isNull();
        assertThat(detail.isReady()).isTrue();
    }

    @Test
    @DisplayName("결제 상세을 승인 진행 상태로 변경한다")
    void markApprovingChangesStatusToApproving() {
        // given
        var detail = PaymentDetail.ready(null, PaymentMethod.CARD, 10_000L, "tx-key");

        // when
        detail.markApproving();

        // then
        assertThat(detail.isApproving()).isTrue();
    }

    @Test
    @DisplayName("결제 상세을 승인 완료 상태로 변경한다")
    void markApprovedChangesStatusToApproved() {
        // given
        var detail = PaymentDetail.ready(null, PaymentMethod.CARD, 10_000L, "tx-key");

        // when
        detail.markApproved();

        // then
        assertThat(detail.getStatus()).isEqualTo(PaymentDetailStatus.APPROVED);
        assertThat(detail.getApprovedAt()).isNotNull();
    }

    @Test
    @DisplayName("결제 상세를 일부 취소하면 남은 금액을 줄인다")
    void cancelPartialAmountReducesRemainingAmount() {
        // given
        var detail = PaymentDetail.ready(null, PaymentMethod.CARD, 10_000L, "tx-key");
        detail.markApproved();

        // when
        detail.cancel(3_000L, "cancel-key", "부분 취소");

        // then
        assertThat(detail.getStatus()).isEqualTo(PaymentDetailStatus.PARTIAL_CANCELED);
        assertThat(detail.getPaymentAmount()).isEqualTo(10_000L);
        assertThat(detail.getCanceledAmount()).isEqualTo(3_000L);
        assertThat(detail.getRemainingAmount()).isEqualTo(7_000L);
        assertThat(detail.getCancelKey()).isEqualTo("cancel-key");
        assertThat(detail.getFailureReason()).isEqualTo("부분 취소");
        assertThat(detail.getCanceledAt()).isNotNull();
    }

    @Test
    @DisplayName("같은 취소 키로 다시 요청하면 취소 금액을 중복 반영하지 않는다")
    void cancelWithSameCancelKeyDoesNotDuplicateCanceledAmount() {
        // given
        var detail = PaymentDetail.ready(null, PaymentMethod.CARD, 10_000L, "tx-key");
        detail.cancel(3_000L, "cancel-key", "부분 취소");

        // when
        detail.cancel(3_000L, "cancel-key", "재시도");

        // then
        assertThat(detail.getStatus()).isEqualTo(PaymentDetailStatus.PARTIAL_CANCELED);
        assertThat(detail.getPaymentAmount()).isEqualTo(10_000L);
        assertThat(detail.getCanceledAmount()).isEqualTo(3_000L);
        assertThat(detail.getRemainingAmount()).isEqualTo(7_000L);
        assertThat(detail.getCancelKey()).isEqualTo("cancel-key");
        assertThat(detail.getFailureReason()).isEqualTo("부분 취소");
    }

    @Test
    @DisplayName("결제 상세를 전액 취소하면 취소 상태로 변경한다")
    void cancelFullAmountChangesStatusToCanceled() {
        // given
        var detail = PaymentDetail.ready(null, PaymentMethod.CARD, 10_000L, "tx-key");

        // when
        detail.cancel(10_000L, "cancel-key", "품절");

        // then
        assertThat(detail.getStatus()).isEqualTo(PaymentDetailStatus.CANCELED);
        assertThat(detail.getPaymentAmount()).isEqualTo(10_000L);
        assertThat(detail.getCanceledAmount()).isEqualTo(10_000L);
        assertThat(detail.getRemainingAmount()).isZero();
        assertThat(detail.getCancelKey()).isEqualTo("cancel-key");
        assertThat(detail.getFailureReason()).isEqualTo("품절");
        assertThat(detail.getCanceledAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 취소된 결제 상세를 다른 취소 키로 다시 취소하면 실패한다")
    void cancelWithDifferentCancelKeyAfterCancelFails() {
        // given
        var detail = PaymentDetail.ready(null, PaymentMethod.CARD, 10_000L, "tx-key");
        detail.cancel(3_000L, "cancel-key", "부분 취소");

        // when & then
        assertThatThrownBy(() -> detail.cancel(7_000L, "other-cancel-key", "잔여 취소"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("같은 취소 키라도 취소 금액이 다르면 실패한다")
    void cancelWithSameCancelKeyAndDifferentAmountFails() {
        // given
        var detail = PaymentDetail.ready(null, PaymentMethod.CARD, 10_000L, "tx-key");
        detail.cancel(3_000L, "cancel-key", "부분 취소");

        // when & then
        assertThatThrownBy(() -> detail.cancel(7_000L, "cancel-key", "다른 금액"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("결제 상세 취소 금액이 남은 금액보다 크면 실패한다")
    void cancelGreaterThanRemainingAmountFails() {
        // given
        var detail = PaymentDetail.ready(null, PaymentMethod.CARD, 10_000L, "tx-key");

        // when & then
        assertThatThrownBy(() -> detail.cancel(10_001L, "cancel-key", "초과 취소"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("취소 키가 없으면 결제 상세 취소에 실패한다")
    void cancelWithoutCancelKeyFails() {
        // given
        var detail = PaymentDetail.ready(null, PaymentMethod.CARD, 10_000L, "tx-key");

        // when & then
        assertThatThrownBy(() -> detail.cancel(10_000L, " ", "취소"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("결제 상세을 실패 상태로 변경한다")
    void markFailedChangesStatusToFailed() {
        // given
        var detail = PaymentDetail.ready(null, PaymentMethod.CARD, 10_000L, "tx-key");

        // when
        detail.markFailed("승인 실패");

        // then
        assertThat(detail.getStatus()).isEqualTo(PaymentDetailStatus.FAILED);
        assertThat(detail.getFailureReason()).isEqualTo("승인 실패");
    }

    @Test
    @DisplayName("외부 거래키가 일치하면 true를 반환한다")
    void matchesExternalTransactionKeyReturnsTrueWhenKeyMatches() {
        // given
        var detail = PaymentDetail.ready(null, PaymentMethod.CARD, 10_000L, "tx-key");

        // when & then
        assertThat(detail.matchesExternalTransactionKey("tx-key")).isTrue();
    }
}
