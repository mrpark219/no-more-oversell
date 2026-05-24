package me.park.nomoreoversell.ordersheet.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderSheetTest {

    @Test
    @DisplayName("숙소 상품 금액을 복사해 주문서를 생성한다")
    void createCopiesProductPriceSnapshot() {
        // given

        // when
        var orderSheet = OrderSheet.create("sheet-token", 1L, 10L, 20_000L, 10_000L);

        // then
        assertThat(orderSheet.getOrderSheetToken()).isEqualTo("sheet-token");
        assertThat(orderSheet.getUserId()).isEqualTo(1L);
        assertThat(orderSheet.getProductId()).isEqualTo(10L);
        assertThat(orderSheet.getOriginalPrice()).isEqualTo(20_000L);
        assertThat(orderSheet.getSalePrice()).isEqualTo(10_000L);
        assertThat(orderSheet.getStatus()).isEqualTo(OrderSheetStatus.CREATED);
    }

    @Test
    @DisplayName("주문서 상태는 현재 주문 생성 흐름에서 사용하는 상태만 가진다")
    void orderSheetStatusContainsOnlyCurrentOrderFlowStates() {
        // given

        // when & then
        assertThat(OrderSheetStatus.values())
                .containsExactly(
                        OrderSheetStatus.CREATED,
                        OrderSheetStatus.APPROVING,
                        OrderSheetStatus.CONFIRMED,
                        OrderSheetStatus.FAILED,
                        OrderSheetStatus.EXPIRED
                );

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
    @DisplayName("주문서를 실패 상태로 변경하고 실패 사유를 저장한다")
    void markFailedChangesStatusToFailedAndStoresFailureReason() {
        // given
        var orderSheet = orderSheet();

        // when
        orderSheet.markFailed(OrderSheetFailureReason.SOLD_OUT);

        // then
        assertThat(orderSheet.getStatus()).isEqualTo(OrderSheetStatus.FAILED);
        assertThat(orderSheet.getFailureReason()).isEqualTo(OrderSheetFailureReason.SOLD_OUT);
    }

    @Test
    @DisplayName("주문서를 확정 상태로 변경하면 실패 사유를 비운다")
    void markConfirmedClearsFailureReason() {
        // given
        var orderSheet = orderSheet();
        orderSheet.markFailed(OrderSheetFailureReason.PAYMENT_FAILED);

        // when
        orderSheet.markConfirmed();

        // then
        assertThat(orderSheet.getStatus()).isEqualTo(OrderSheetStatus.CONFIRMED);
        assertThat(orderSheet.getFailureReason()).isNull();
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
}
