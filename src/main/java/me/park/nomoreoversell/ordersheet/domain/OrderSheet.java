package me.park.nomoreoversell.ordersheet.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.park.nomoreoversell.common.domain.BaseTimeEntity;
import me.park.nomoreoversell.stayproduct.doamin.StayProduct;

@Entity
@Table(
        name = "order_sheet",
        uniqueConstraints = {
                @UniqueConstraint(name = "UNI_ORDER_SHEET_TOKEN", columnNames = "order_sheet_token")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderSheet extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String orderSheetToken;

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
    private OrderSheetStatus status;

    @Builder
    private OrderSheet(
            String orderSheetToken,
            Long userId,
            Long productId,
            Long originalPrice,
            Long salePrice,
            OrderSheetStatus status
    ) {
        this.orderSheetToken = orderSheetToken;
        this.userId = userId;
        this.productId = productId;
        this.originalPrice = originalPrice;
        this.salePrice = salePrice;
        this.status = status;
    }

    public static OrderSheet create(String orderSheetToken, Long userId, StayProduct product) {
        return OrderSheet.builder()
                .orderSheetToken(orderSheetToken)
                .userId(userId)
                .productId(product.getId())
                .originalPrice(product.getOriginalPrice())
                .salePrice(product.getSalePrice())
                .status(OrderSheetStatus.CREATED)
                .build();
    }

    public void markReady() {
        this.status = OrderSheetStatus.READY;
    }

    public void markApproving() {
        this.status = OrderSheetStatus.APPROVING;
    }

    public void markConfirmed() {
        this.status = OrderSheetStatus.CONFIRMED;
    }

    public void markSoldOut() {
        this.status = OrderSheetStatus.SOLD_OUT;
    }

    public void markPaymentFailed() {
        this.status = OrderSheetStatus.PAYMENT_FAILED;
    }

    public boolean isReady() {
        return status == OrderSheetStatus.READY;
    }

    public boolean isApproving() {
        return status == OrderSheetStatus.APPROVING;
    }

    public boolean isConfirmed() {
        return status == OrderSheetStatus.CONFIRMED;
    }
}
