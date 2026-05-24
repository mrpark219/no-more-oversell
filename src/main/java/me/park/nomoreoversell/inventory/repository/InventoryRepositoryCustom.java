package me.park.nomoreoversell.inventory.repository;

import me.park.nomoreoversell.inventory.domain.Inventory;

import java.util.Optional;

public interface InventoryRepositoryCustom {

    Optional<Inventory> getByProductIdWithLock(Long productId);
}
