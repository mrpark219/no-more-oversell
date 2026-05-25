package me.park.nomoreoversell.ordersheet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.park.nomoreoversell.inventory.service.InventoryService;
import me.park.nomoreoversell.stayproduct.service.StayProductService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CheckoutCacheWarmUpRunner implements ApplicationRunner {

    private final CheckoutCacheWarmUpProperties properties;
    private final StayProductService stayProductService;
    private final InventoryService inventoryService;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }

        for (Long productId : properties.getProductIds()) {
            warmUp(productId);
        }
    }

    private void warmUp(Long productId) {
        try {
            stayProductService.warmUp(productId);
            inventoryService.warmUpSoldOutHint(productId);
            log.info("체크아웃 캐시 웜업 완료. productId={}", productId);
        } catch (RuntimeException e) {
            log.warn("체크아웃 캐시 웜업 실패. productId={}", productId, e);
        }
    }
}
