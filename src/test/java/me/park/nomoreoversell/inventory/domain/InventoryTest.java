package me.park.nomoreoversell.inventory.domain;

import me.park.nomoreoversell.exception.InsufficientSoldQuantityException;
import me.park.nomoreoversell.exception.InvalidInventoryQuantityException;
import me.park.nomoreoversell.exception.SoldOutException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class InventoryTest {

    @Test
    @DisplayName("판매 수량이 전체 수량보다 작으면 품절이 아니다")
    void isSoldOutReturnsFalseWhenAvailableQuantityExists() {
        // given
        var inventory = inventory(10L, 9L);

        // when
        var result = inventory.isSoldOut();

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("판매 수량이 전체 수량과 같으면 품절이다")
    void isSoldOutReturnsTrueWhenSoldQuantityEqualsTotalQuantity() {
        // given
        var inventory = inventory(10L, 10L);

        // when
        var result = inventory.isSoldOut();

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("남은 재고 수량을 계산한다")
    void availableQuantityReturnsRemainingQuantity() {
        // given
        var inventory = inventory(10L, 3L);

        // when
        var result = inventory.availableQuantity();

        // then
        assertThat(result).isEqualTo(7L);
    }

    @Test
    @DisplayName("요청 수량만큼 재고가 남아 있으면 true를 반환한다")
    void hasQuantityReturnsTrueWhenAvailableQuantityIsEnough() {
        // given
        var inventory = inventory(10L, 3L);

        // when
        var result = inventory.hasQuantity(7L);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("요청 수량보다 재고가 부족하면 false를 반환한다")
    void hasQuantityReturnsFalseWhenAvailableQuantityIsNotEnough() {
        // given
        var inventory = inventory(10L, 3L);

        // when
        var result = inventory.hasQuantity(8L);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("재고가 남아 있으면 1개를 예약한다")
    void reserveOneIncreasesSoldQuantityWhenStockExists() {
        // given
        var inventory = inventory(10L, 0L);

        // when
        inventory.reserveOne();

        // then
        assertThat(inventory.getSoldQuantity()).isEqualTo(1L);
        assertThat(inventory.availableQuantity()).isEqualTo(9L);
    }

    @Test
    @DisplayName("재고가 남아 있으면 요청 수량만큼 예약한다")
    void reserveIncreasesSoldQuantityByQuantity() {
        // given
        var inventory = inventory(10L, 2L);

        // when
        inventory.reserve(3L);

        // then
        assertThat(inventory.getSoldQuantity()).isEqualTo(5L);
        assertThat(inventory.availableQuantity()).isEqualTo(5L);
    }

    @Test
    @DisplayName("재고가 부족하면 예약할 수 없다")
    void reserveThrowsSoldOutExceptionWhenStockIsNotEnough() {
        // given
        var inventory = inventory(10L, 10L);

        // when
        var thrown = catchThrowable(inventory::reserveOne);

        // then
        assertThat(thrown)
                .isInstanceOf(SoldOutException.class)
                .hasMessage("품절된 상품입니다.");
    }

    @Test
    @DisplayName("0 이하 수량은 예약할 수 없다")
    void reserveThrowsExceptionWhenQuantityIsNotPositive() {
        // given
        var inventory = inventory(10L, 0L);

        // when
        var thrown = catchThrowable(() -> inventory.reserve(0L));

        // then
        assertThat(thrown)
                .isInstanceOf(InvalidInventoryQuantityException.class)
                .hasMessage("재고 수량은 1개 이상이어야 합니다.");
    }

    @Test
    @DisplayName("판매 수량이 충분하면 재고를 복구한다")
    void restoreOneDecreasesSoldQuantityWhenSoldQuantityExists() {
        // given
        var inventory = inventory(10L, 1L);

        // when
        inventory.restoreOne();

        // then
        assertThat(inventory.getSoldQuantity()).isZero();
        assertThat(inventory.availableQuantity()).isEqualTo(10L);
    }

    @Test
    @DisplayName("판매 수량이 충분하면 요청 수량만큼 재고를 복구한다")
    void restoreDecreasesSoldQuantityByQuantity() {
        // given
        var inventory = inventory(10L, 5L);

        // when
        inventory.restore(3L);

        // then
        assertThat(inventory.getSoldQuantity()).isEqualTo(2L);
        assertThat(inventory.availableQuantity()).isEqualTo(8L);
    }

    @Test
    @DisplayName("복구할 판매 수량이 부족하면 재고를 복구할 수 없다")
    void restoreThrowsExceptionWhenSoldQuantityIsNotEnough() {
        // given
        var inventory = inventory(10L, 0L);

        // when
        var thrown = catchThrowable(inventory::restoreOne);

        // then
        assertThat(thrown)
                .isInstanceOf(InsufficientSoldQuantityException.class)
                .hasMessage("복구할 판매 수량이 부족합니다.");
    }

    @Test
    @DisplayName("0 이하 수량은 재고 복구에 사용할 수 없다")
    void restoreThrowsExceptionWhenQuantityIsNotPositive() {
        // given
        var inventory = inventory(10L, 1L);

        // when
        var thrown = catchThrowable(() -> inventory.restore(0L));

        // then
        assertThat(thrown)
                .isInstanceOf(InvalidInventoryQuantityException.class)
                .hasMessage("재고 수량은 1개 이상이어야 합니다.");
    }

    @Test
    @DisplayName("0 이하 수량은 재고 검증에 사용할 수 없다")
    void hasQuantityThrowsExceptionWhenQuantityIsNotPositive() {
        // given
        var inventory = inventory(10L, 0L);

        // when
        var thrown = catchThrowable(() -> inventory.hasQuantity(0L));

        // then
        assertThat(thrown)
                .isInstanceOf(InvalidInventoryQuantityException.class)
                .hasMessage("재고 수량은 1개 이상이어야 합니다.");
    }

    private Inventory inventory(long totalQuantity, long soldQuantity) {
        return Inventory.builder()
                .productId(1L)
                .totalQuantity(totalQuantity)
                .soldQuantity(soldQuantity)
                .build();
    }
}
