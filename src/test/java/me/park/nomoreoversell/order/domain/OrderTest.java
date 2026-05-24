package me.park.nomoreoversell.order.domain;

import me.park.nomoreoversell.ordersheet.domain.OrderSheet;
import me.park.nomoreoversell.ordersheet.domain.OrderSheetStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {

    @Test
    @DisplayName("주문서 금액 정보를 복사해 확정 주문을 생성한다")
    void confirmedCopiesOrderSheetPriceSnapshot() {
        var orderSheet = OrderSheet.builder()
                .orderSheetToken("sheet-token")
                .userId(1L)
                .productId(10L)
                .originalPrice(20_000L)
                .salePrice(10_000L)
                .status(OrderSheetStatus.CONFIRMED)
                .build();
        ReflectionTestUtils.setField(orderSheet, "id", 100L);

        var order = Order.confirmed(orderSheet);

        assertThat(order.getOrderSheetId()).isEqualTo(100L);
        assertThat(order.getUserId()).isEqualTo(1L);
        assertThat(order.getProductId()).isEqualTo(10L);
        assertThat(order.getOriginalPrice()).isEqualTo(20_000L);
        assertThat(order.getSalePrice()).isEqualTo(10_000L);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }
}
