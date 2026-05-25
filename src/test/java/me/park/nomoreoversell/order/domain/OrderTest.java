package me.park.nomoreoversell.order.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {

    @Test
    @DisplayName("주문서 금액 정보를 복사해 확정 주문을 생성한다")
    void confirmedCopiesOrderSheetPriceSnapshot() {
        var order = Order.confirmed(100L, 1L, 10L, 20_000L, 10_000L);

        assertThat(order.getOrderSheetId()).isEqualTo(100L);
        assertThat(order.getOrderToken()).isNotBlank();
        assertThat(order.getUserId()).isEqualTo(1L);
        assertThat(order.getProductId()).isEqualTo(10L);
        assertThat(order.getOriginalPrice()).isEqualTo(20_000L);
        assertThat(order.getSalePrice()).isEqualTo(10_000L);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }
}
