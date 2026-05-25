package me.park.nomoreoversell.order.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.park.nomoreoversell.common.domain.BaseTimeEntity;
import me.park.nomoreoversell.ordersheet.domain.OrderSheet;

import java.util.UUID;

@Entity
@Table(
        name = "orders",
        uniqueConstraints = {
                @UniqueConstraint(name = "UNI_ORDERS_ORDER_SHEET_ID", columnNames = "order_sheet_id"),
                @UniqueConstraint(name = "UNI_ORDERS_ORDER_TOKEN", columnNames = "order_token")
        },
        indexes = {
                @Index(name = "IDX_ORDERS_USER_PRODUCT_STATUS", columnList = "user_id, product_id, status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String orderToken;

    @Column(nullable = false)
    private Long orderSheetId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Long originalPrice;

    @Column(nullable = false)
    private Long salePrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Builder
    private Order(
            String orderToken,
            Long orderSheetId,
            Long userId,
            Long productId,
            Long originalPrice,
            Long salePrice,
            OrderStatus status
    ) {
        this.orderToken = orderToken;
        this.orderSheetId = orderSheetId;
        this.userId = userId;
        this.productId = productId;
        this.originalPrice = originalPrice;
        this.salePrice = salePrice;
        this.status = status;
    }

    public static Order confirmed(OrderSheet orderSheet) {
        return Order.builder()
                .orderToken(UUID.randomUUID().toString())
                .orderSheetId(orderSheet.getId())
                .userId(orderSheet.getUserId())
                .productId(orderSheet.getProductId())
                .originalPrice(orderSheet.getOriginalPrice())
                .salePrice(orderSheet.getSalePrice())
                .status(OrderStatus.CONFIRMED)
                .build();
    }
}
