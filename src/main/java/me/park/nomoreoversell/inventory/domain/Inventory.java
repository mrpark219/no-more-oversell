package me.park.nomoreoversell.inventory.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.park.nomoreoversell.common.domain.BaseTimeEntity;
import me.park.nomoreoversell.exception.InsufficientSoldQuantityException;
import me.park.nomoreoversell.exception.InvalidInventoryQuantityException;
import me.park.nomoreoversell.exception.SoldOutException;

@Entity
@Table(
        name = "inventory",
        uniqueConstraints = {
                @UniqueConstraint(name = "UNI_INVENTORY_PRODUCT_ID", columnNames = "product_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inventory extends BaseTimeEntity {

    private static final long RESERVE_UNIT = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Long totalQuantity;

    @Column(nullable = false)
    private Long soldQuantity;

    @Builder
    public Inventory(Long productId, long totalQuantity, long soldQuantity) {
        this.productId = productId;
        this.totalQuantity = totalQuantity;
        this.soldQuantity = soldQuantity;
    }

    public boolean isSoldOut() {
        return soldQuantity >= totalQuantity;
    }

    public long availableQuantity() {
        return totalQuantity - soldQuantity;
    }

    public boolean hasQuantity(long quantity) {
        validatePositiveQuantity(quantity);
        return availableQuantity() >= quantity;
    }

    public void reserveOne() {
        reserve(RESERVE_UNIT);
    }

    public void reserve(long quantity) {
        if (!hasQuantity(quantity)) {
            throw new SoldOutException();
        }
        this.soldQuantity += quantity;
    }

    public void restoreOne() {
        restore(RESERVE_UNIT);
    }

    public void restore(long quantity) {
        validatePositiveQuantity(quantity);
        if (soldQuantity < quantity) {
            throw new InsufficientSoldQuantityException();
        }
        this.soldQuantity -= quantity;
    }

    private void validatePositiveQuantity(long quantity) {
        if (quantity <= 0) {
            throw new InvalidInventoryQuantityException();
        }
    }
}
