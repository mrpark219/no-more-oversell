package me.park.nomoreoversell.inventory.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import me.park.nomoreoversell.inventory.domain.Inventory;

import java.util.Optional;

import static me.park.nomoreoversell.inventory.domain.QInventory.inventory;

@RequiredArgsConstructor
public class InventoryRepositoryCustomImpl implements InventoryRepositoryCustom {

    private final JPAQueryFactory factory;

    @Override
    public Optional<Inventory> getByProductIdWithLock(Long productId) {
        return Optional.ofNullable(
                factory.selectFrom(inventory)
                        .where(inventory.productId.eq(productId))
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .fetchOne()
        );
    }
}
