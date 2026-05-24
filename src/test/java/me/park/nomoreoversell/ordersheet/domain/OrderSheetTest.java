package me.park.nomoreoversell.ordersheet.domain;

import me.park.nomoreoversell.stayproduct.doamin.StayProduct;
import me.park.nomoreoversell.stayproduct.doamin.StayProductStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class OrderSheetTest {

    @Test
    @DisplayName("숙소 상품 금액을 복사해 주문서를 생성한다")
    void createCopiesProductPriceSnapshot() {
        // given
        var product = stayProduct();
        ReflectionTestUtils.setField(product, "id", 10L);

        // when
        var orderSheet = OrderSheet.create("sheet-token", 1L, product);

        // then
        assertThat(orderSheet.getOrderSheetToken()).isEqualTo("sheet-token");
        assertThat(orderSheet.getUserId()).isEqualTo(1L);
        assertThat(orderSheet.getProductId()).isEqualTo(10L);
        assertThat(orderSheet.getOriginalPrice()).isEqualTo(20_000L);
        assertThat(orderSheet.getSalePrice()).isEqualTo(10_000L);
        assertThat(orderSheet.getStatus()).isEqualTo(OrderSheetStatus.CREATED);
    }

    @Test
    @DisplayName("주문서를 결제 준비 상태로 변경한다")
    void markReadyChangesStatusToReady() {
        // given
        var orderSheet = orderSheet();

        // when
        orderSheet.markReady();

        // then
        assertThat(orderSheet.isReady()).isTrue();
    }

    @Test
    @DisplayName("주문서가 결제 준비 상태가 아니면 false를 반환한다")
    void isReadyReturnsFalseWhenStatusIsNotReady() {
        // given
        var orderSheet = orderSheet();

        // when
        var result = orderSheet.isReady();

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("주문서를 승인 진행 상태로 변경한다")
    void markApprovingChangesStatusToApproving() {
        // given
        var orderSheet = orderSheet();

        // when
        orderSheet.markApproving();

        // then
        assertThat(orderSheet.isApproving()).isTrue();
    }

    @Test
    @DisplayName("주문서가 승인 진행 상태가 아니면 false를 반환한다")
    void isApprovingReturnsFalseWhenStatusIsNotApproving() {
        // given
        var orderSheet = orderSheet();

        // when
        var result = orderSheet.isApproving();

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("주문서를 확정 상태로 변경한다")
    void markConfirmedChangesStatusToConfirmed() {
        // given
        var orderSheet = orderSheet();

        // when
        orderSheet.markConfirmed();

        // then
        assertThat(orderSheet.isConfirmed()).isTrue();
    }

    @Test
    @DisplayName("주문서가 확정 상태가 아니면 false를 반환한다")
    void isConfirmedReturnsFalseWhenStatusIsNotConfirmed() {
        // given
        var orderSheet = orderSheet();

        // when
        var result = orderSheet.isConfirmed();

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("주문서를 품절 상태로 변경한다")
    void markSoldOutChangesStatusToSoldOut() {
        // given
        var orderSheet = orderSheet();

        // when
        orderSheet.markSoldOut();

        // then
        assertThat(orderSheet.getStatus()).isEqualTo(OrderSheetStatus.SOLD_OUT);
    }

    @Test
    @DisplayName("주문서를 결제 실패 상태로 변경한다")
    void markPaymentFailedChangesStatusToPaymentFailed() {
        // given
        var orderSheet = orderSheet();

        // when
        orderSheet.markPaymentFailed();

        // then
        assertThat(orderSheet.getStatus()).isEqualTo(OrderSheetStatus.PAYMENT_FAILED);
    }

    private OrderSheet orderSheet() {
        return OrderSheet.builder()
                .orderSheetToken("sheet-token")
                .userId(1L)
                .productId(10L)
                .originalPrice(20_000L)
                .salePrice(10_000L)
                .status(OrderSheetStatus.CREATED)
                .build();
    }

    private StayProduct stayProduct() {
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
                .maxPerUser(1L)
                .build();
    }
}
