package me.park.nomoreoversell.ordersheet.service;

import me.park.nomoreoversell.inventory.service.InventoryService;
import me.park.nomoreoversell.stayproduct.service.StayProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CheckoutCacheWarmUpRunnerTest {

    @Mock
    private StayProductService stayProductService;

    @Mock
    private InventoryService inventoryService;

    @Test
    @DisplayName("웜업이 비활성화되어 있으면 상품과 재고 캐시를 적재하지 않는다")
    void runDoesNothingWhenWarmUpIsDisabled() throws Exception {
        // given
        var properties = new CheckoutCacheWarmUpProperties();
        properties.setEnabled(false);
        properties.setProductIds(List.of(1L, 2L));
        var runner = new CheckoutCacheWarmUpRunner(properties, stayProductService, inventoryService);

        // when
        runner.run(null);

        // then
        verifyNoInteractions(stayProductService, inventoryService);
    }

    @Test
    @DisplayName("웜업이 활성화되어 있으면 지정된 상품 정보와 마감 힌트를 적재한다")
    void runWarmsUpProductAndSoldOutHintCaches() throws Exception {
        // given
        var properties = new CheckoutCacheWarmUpProperties();
        properties.setEnabled(true);
        properties.setProductIds(List.of(1L, 2L));
        var runner = new CheckoutCacheWarmUpRunner(properties, stayProductService, inventoryService);

        // when
        runner.run(null);

        // then
        verify(stayProductService).warmUpCache(1L);
        verify(stayProductService).warmUpCache(2L);
        verify(inventoryService).warmUpSoldOutHintCache(1L);
        verify(inventoryService).warmUpSoldOutHintCache(2L);
    }
}
