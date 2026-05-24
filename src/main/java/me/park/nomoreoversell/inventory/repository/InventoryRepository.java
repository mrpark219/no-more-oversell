package me.park.nomoreoversell.inventory.repository;

import me.park.nomoreoversell.inventory.domain.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long>, InventoryRepositoryCustom {

    Optional<Inventory> findByProductId(Long productId);
}
