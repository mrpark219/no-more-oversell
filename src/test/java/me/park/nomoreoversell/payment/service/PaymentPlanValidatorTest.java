package me.park.nomoreoversell.payment.service;

import me.park.nomoreoversell.exception.InvalidPaymentCombinationException;
import me.park.nomoreoversell.payment.domain.PaymentMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentPlanValidatorTest {

    private final PaymentPlanValidator paymentPlanValidator = new PaymentPlanValidator();

    @Test
    @DisplayName("결제 상세 합계가 판매가와 같으면 검증에 성공한다")
    void validateSucceedsWhenPaymentAmountEqualsSalePrice() {
        paymentPlanValidator.validate(10_000L, List.of(
                new PaymentDetailRequest(PaymentMethod.CARD, 7_000L),
                new PaymentDetailRequest(PaymentMethod.POINT, 3_000L)
        ));
    }

    @Test
    @DisplayName("PG 결제수단이 2개면 결제 조합 검증에 실패한다")
    void validateThrowsExceptionWhenPgPaymentMethodCountIsGreaterThanOne() {
        assertThatThrownBy(() -> paymentPlanValidator.validate(10_000L, List.of(
                new PaymentDetailRequest(PaymentMethod.CARD, 5_000L),
                new PaymentDetailRequest(PaymentMethod.Y_PAY, 5_000L)
        ))).isInstanceOf(InvalidPaymentCombinationException.class)
                .hasMessage("주결제수단은 1개까지만 사용할 수 있습니다.");
    }

    @Test
    @DisplayName("같은 결제수단이 중복되면 결제 조합 검증에 실패한다")
    void validateThrowsExceptionWhenPaymentMethodIsDuplicated() {
        assertThatThrownBy(() -> paymentPlanValidator.validate(10_000L, List.of(
                new PaymentDetailRequest(PaymentMethod.POINT, 5_000L),
                new PaymentDetailRequest(PaymentMethod.POINT, 5_000L)
        ))).isInstanceOf(InvalidPaymentCombinationException.class)
                .hasMessage("동일한 결제수단을 중복으로 사용할 수 없습니다.");
    }

    @Test
    @DisplayName("결제 상세 합계가 판매가와 다르면 검증에 실패한다")
    void validateThrowsExceptionWhenPaymentAmountDoesNotEqualSalePrice() {
        assertThatThrownBy(() -> paymentPlanValidator.validate(10_000L, List.of(
                new PaymentDetailRequest(PaymentMethod.CARD, 9_000L)
        ))).isInstanceOf(InvalidPaymentCombinationException.class);
    }
}
