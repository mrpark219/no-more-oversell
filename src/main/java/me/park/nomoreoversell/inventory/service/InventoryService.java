package me.park.nomoreoversell.inventory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.park.nomoreoversell.exception.InsufficientSoldQuantityException;
import me.park.nomoreoversell.exception.InvalidInventoryQuantityException;
import me.park.nomoreoversell.exception.InventoryNotFoundException;
import me.park.nomoreoversell.exception.SoldOutException;
import me.park.nomoreoversell.inventory.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    @Transactional
    public void reserveOne(Long productId) {
        reserve(productId, 1L);
    }

    @Transactional
    public void reserve(Long productId, long quantity) {
        log.info("재고 예약 시도. productId={}, quantity={}", productId, quantity);
        validateQuantity(quantity, "재고 예약");

        var inventory = inventoryRepository.getByProductIdWithLock(productId)
                .orElseThrow(() -> {
                    log.warn("재고 예약 실패: 재고가 존재하지 않습니다. productId={}, quantity={}", productId, quantity);
                    return new InventoryNotFoundException();
                });

        if (!inventory.hasQuantity(quantity)) {
            log.info(
                    "재고 예약 실패: 재고가 부족합니다. productId={}, quantity={}, totalQuantity={}, soldQuantity={}, available={}",
                    productId,
                    quantity,
                    inventory.getTotalQuantity(),
                    inventory.getSoldQuantity(),
                    inventory.availableQuantity()
            );
            throw new SoldOutException();
        }

        inventory.reserve(quantity);
        log.info(
                "재고 예약 성공. productId={}, quantity={}, totalQuantity={}, soldQuantity={}, available={}",
                productId,
                quantity,
                inventory.getTotalQuantity(),
                inventory.getSoldQuantity(),
                inventory.availableQuantity()
        );
    }

    @Transactional(readOnly = true)
    public boolean hasStock(Long productId) {
        return hasStock(productId, 1L);
    }

    @Transactional(readOnly = true)
    public boolean hasStock(Long productId, long quantity) {
        log.debug("재고 여부 조회 시도. productId={}, quantity={}", productId, quantity);
        validateQuantity(quantity, "재고 조회");

        var hasStock = inventoryRepository.findByProductId(productId)
                .map(inventory -> inventory.hasQuantity(quantity))
                .orElse(false);

        log.debug("재고 여부 조회 완료. productId={}, quantity={}, hasStock={}", productId, quantity, hasStock);
        return hasStock;
    }

    @Transactional
    public void restoreOne(Long productId) {
        restore(productId, 1L);
    }

    @Transactional
    public void restore(Long productId, long quantity) {
        log.info("재고 복구 시도. productId={}, quantity={}", productId, quantity);
        validateQuantity(quantity, "재고 복구");

        var inventory = inventoryRepository.getByProductIdWithLock(productId)
                .orElseThrow(() -> {
                    log.warn("재고 복구 실패: 재고가 존재하지 않습니다. productId={}, quantity={}", productId, quantity);
                    return new InventoryNotFoundException();
                });

        if (inventory.getSoldQuantity() < quantity) {
            log.warn(
                    "재고 복구 실패: 복구할 판매 수량이 부족합니다. productId={}, quantity={}, soldQuantity={}",
                    productId,
                    quantity,
                    inventory.getSoldQuantity()
            );
            throw new InsufficientSoldQuantityException();
        }

        inventory.restore(quantity);
        log.info(
                "재고 복구 성공. productId={}, quantity={}, totalQuantity={}, soldQuantity={}, available={}",
                productId,
                quantity,
                inventory.getTotalQuantity(),
                inventory.getSoldQuantity(),
                inventory.availableQuantity()
        );
    }

    private void validateQuantity(long quantity, String operation) {
        if (quantity <= 0) {
            log.warn("{} 실패: 수량은 1개 이상이어야 합니다. quantity={}", operation, quantity);
            throw new InvalidInventoryQuantityException();
        }
    }
}
