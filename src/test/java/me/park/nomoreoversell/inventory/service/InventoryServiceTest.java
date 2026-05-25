package me.park.nomoreoversell.inventory.service;

import me.park.nomoreoversell.exception.InventoryNotFoundException;
import me.park.nomoreoversell.exception.SoldOutException;
import me.park.nomoreoversell.inventory.domain.Inventory;
import me.park.nomoreoversell.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProductSoldOutCache productSoldOutCache;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    @DisplayName("재고가 있으면 1개를 예약한다")
    void reserveOneStockIncreasesSoldQuantityWhenInventoryExists() {
        // given
        var inventory = inventory(10L, 0L);
        given(inventoryRepository.getByProductIdWithLock(1L))
                .willReturn(Optional.of(inventory));

        // when
        inventoryService.reserveOneStock(1L);

        // then
        assertThat(inventory.getSoldQuantity()).isEqualTo(1L);
    }

    @Test
    @DisplayName("재고 예약으로 판매 가능 수량이 0개가 되면 마감 힌트 캐시를 저장한다")
    void reserveOneStockMarksSoldOutCacheWhenRemainingQuantityBecomesZero() {
        // given
        var inventory = inventory(10L, 9L);
        given(inventoryRepository.getByProductIdWithLock(1L))
                .willReturn(Optional.of(inventory));

        // when
        inventoryService.reserveOneStock(1L);

        // then
        assertThat(inventory.getSoldQuantity()).isEqualTo(10L);
        verify(productSoldOutCache).markSoldOut(1L);
    }

    @Test
    @DisplayName("재고가 없으면 예약할 수 없다")
    void reserveOneStockThrowsExceptionWhenInventoryDoesNotExist() {
        // given
        given(inventoryRepository.getByProductIdWithLock(1L))
                .willReturn(Optional.empty());

        // when
        var thrown = catchThrowable(() -> inventoryService.reserveOneStock(1L));

        // then
        assertThat(thrown)
                .isInstanceOf(InventoryNotFoundException.class);
    }

    @Test
    @DisplayName("판매 가능한 재고가 없으면 예약할 수 없다")
    void reserveOneStockThrowsExceptionWhenInventoryIsSoldOut() {
        // given
        var inventory = inventory(10L, 10L);
        given(inventoryRepository.getByProductIdWithLock(1L))
                .willReturn(Optional.of(inventory));

        // when
        var thrown = catchThrowable(() -> inventoryService.reserveOneStock(1L));

        // then
        assertThat(thrown)
                .isInstanceOf(SoldOutException.class);
        verify(productSoldOutCache).markSoldOut(1L);
    }

    @Test
    @DisplayName("재고가 있으면 판매 가능 여부를 true로 반환한다")
    void hasAvailableStockReturnsTrueWhenInventoryHasQuantity() {
        // given
        given(productSoldOutCache.isSoldOut(1L))
                .willReturn(false);
        given(inventoryRepository.findByProductId(1L))
                .willReturn(Optional.of(inventory(10L, 9L)));

        // when
        var result = inventoryService.hasAvailableStock(1L);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("재고가 없으면 판매 가능 여부를 false로 반환한다")
    void hasAvailableStockReturnsFalseWhenInventoryDoesNotExist() {
        // given
        given(productSoldOutCache.isSoldOut(1L))
                .willReturn(false);
        given(inventoryRepository.findByProductId(1L))
                .willReturn(Optional.empty());

        // when
        var result = inventoryService.hasAvailableStock(1L);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("마감 힌트 캐시가 있으면 DB를 조회하지 않고 판매 불가로 반환한다")
    void hasAvailableStockReturnsFalseWithoutRepositoryLookupWhenSoldOutCacheExists() {
        // given
        given(productSoldOutCache.isSoldOut(1L))
                .willReturn(true);

        // when
        var result = inventoryService.hasAvailableStock(1L);

        // then
        assertThat(result).isFalse();
        verifyNoInteractions(inventoryRepository);
    }

    @Test
    @DisplayName("판매 수량이 있으면 재고를 복구한다")
    void restoreOneStockDecreasesSoldQuantityWhenSoldQuantityExists() {
        // given
        var inventory = inventory(10L, 1L);
        given(inventoryRepository.getByProductIdWithLock(1L))
                .willReturn(Optional.of(inventory));

        // when
        inventoryService.restoreOneStock(1L);

        // then
        assertThat(inventory.getSoldQuantity()).isZero();
        verify(productSoldOutCache).evict(1L);
    }

    private Inventory inventory(long totalQuantity, long soldQuantity) {
        return Inventory.builder()
                .productId(1L)
                .totalQuantity(totalQuantity)
                .soldQuantity(soldQuantity)
                .build();
    }
}
