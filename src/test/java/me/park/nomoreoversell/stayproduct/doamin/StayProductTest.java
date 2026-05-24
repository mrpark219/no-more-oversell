package me.park.nomoreoversell.stayproduct.doamin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class StayProductTest {

    @Test
    @DisplayName("인당 구매 제한 수량이 있으면 구매 제한 상품이다")
    void hasPurchaseLimitReturnsTrueWhenMaxPerUserExists() {
        // given
        var product = stayProduct(1L);

        // when
        var result = product.hasPurchaseLimit();

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("인당 구매 제한 수량이 없으면 구매 제한 상품이 아니다")
    void hasPurchaseLimitReturnsFalseWhenMaxPerUserDoesNotExist() {
        // given
        var product = stayProduct(null);

        // when
        var result = product.hasPurchaseLimit();

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("오픈 상태이고 오픈 시간이 지나면 구매 가능한 상품이다")
    void isOpenAtReturnsTrueWhenStatusIsOpenAndOpenAtHasPassed() {
        // given
        var product = stayProduct(null);

        // when
        var result = product.isOpenAt(LocalDateTime.now());

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("오픈 시간이 지나지 않으면 구매 가능한 상품이 아니다")
    void isOpenAtReturnsFalseBeforeOpenAt() {
        // given
        var product = StayProduct.builder()
                .accommodationName("테스트 호텔")
                .roomName("디럭스")
                .ratePlanName("특가")
                .originalPrice(20_000L)
                .salePrice(10_000L)
                .openAt(LocalDateTime.now().plusDays(1))
                .status(StayProductStatus.OPEN)
                .checkinTime(LocalTime.of(15, 0))
                .checkoutTime(LocalTime.of(11, 0))
                .maxPerUser(null)
                .build();

        // when
        var result = product.isOpenAt(LocalDateTime.now());

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("닫힌 상태이면 오픈 시간이 지났어도 구매 가능한 상품이 아니다")
    void isOpenAtReturnsFalseWhenStatusIsClosed() {
        // given
        var product = stayProduct(null);
        product.close();

        // when
        var result = product.isOpenAt(LocalDateTime.now());

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("닫힌 상품을 오픈 상태로 변경한다")
    void openChangesStatusToOpen() {
        // given
        var product = stayProduct(null);
        product.close();

        // when
        product.open();

        // then
        assertThat(product.getStatus()).isEqualTo(StayProductStatus.OPEN);
    }

    @Test
    @DisplayName("상품을 닫힌 상태로 변경한다")
    void closeChangesStatusToClosed() {
        // given
        var product = stayProduct(null);

        // when
        product.close();

        // then
        assertThat(product.getStatus()).isEqualTo(StayProductStatus.CLOSED);
    }

    private StayProduct stayProduct(Long maxPerUser) {
        return StayProduct.builder()
                .accommodationName("테스트 호텔")
                .roomName("디럭스")
                .ratePlanName("특가")
                .originalPrice(20_000L)
                .salePrice(10_000L)
                .openAt(LocalDateTime.now().minusDays(1))
                .status(StayProductStatus.OPEN)
                .checkinTime(LocalTime.of(15, 0))
                .checkoutTime(LocalTime.of(11, 0))
                .maxPerUser(maxPerUser)
                .build();
    }
}
