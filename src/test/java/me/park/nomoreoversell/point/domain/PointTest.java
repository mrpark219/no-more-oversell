package me.park.nomoreoversell.point.domain;

import me.park.nomoreoversell.exception.InsufficientPointBalanceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class PointTest {

    @Test
    @DisplayName("잔액이 요청 금액 이상이면 결제 가능하다")
    void canAffordReturnsTrueWhenBalanceIsEnough() {
        // given
        var point = point(10_000L);

        // when
        var result = point.canAfford(10_000L);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("잔액이 요청 금액보다 작으면 결제할 수 없다")
    void canAffordReturnsFalseWhenBalanceIsNotEnough() {
        // given
        var point = point(1_000L);

        // when
        var result = point.canAfford(3_000L);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("잔액이 충분하면 포인트를 차감한다")
    void deductDecreasesBalanceWhenBalanceIsEnough() {
        // given
        var point = point(10_000L);

        // when
        point.deduct(3_000L);

        // then
        assertThat(point.getBalance()).isEqualTo(7_000L);
    }

    @Test
    @DisplayName("잔액이 부족하면 포인트를 차감할 수 없다")
    void deductThrowsExceptionWhenBalanceIsNotEnough() {
        // given
        var point = point(1_000L);

        // when
        var thrown = catchThrowable(() -> point.deduct(3_000L));

        // then
        assertThat(thrown)
                .isInstanceOf(InsufficientPointBalanceException.class)
                .hasMessage("포인트 잔액이 부족합니다.");
    }

    @Test
    @DisplayName("포인트를 복구한다")
    void restoreIncreasesBalance() {
        // given
        var point = point(7_000L);

        // when
        point.restore(3_000L);

        // then
        assertThat(point.getBalance()).isEqualTo(10_000L);
    }

    private Point point(long balance) {
        return Point.builder()
                .userId(1L)
                .balance(balance)
                .build();
    }
}
