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

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    @DisplayName("재고가 있으면 1개를 예약한다")
    void reserveOneIncreasesSoldQuantityWhenInventoryExists() {
        // given
        var inventory = inventory(10L, 0L);
        given(inventoryRepository.getByProductIdWithLock(1L))
                .willReturn(Optional.of(inventory));

        // when
        inventoryService.reserveOne(1L);

        // then
        assertThat(inventory.getSoldQuantity()).isEqualTo(1L);
    }

    @Test
    @DisplayName("재고가 없으면 예약할 수 없다")
    void reserveOneThrowsExceptionWhenInventoryDoesNotExist() {
        // given
        given(inventoryRepository.getByProductIdWithLock(1L))
                .willReturn(Optional.empty());

        // when
        var thrown = catchThrowable(() -> inventoryService.reserveOne(1L));

        // then
        assertThat(thrown)
                .isInstanceOf(InventoryNotFoundException.class);
    }

    @Test
    @DisplayName("판매 가능한 재고가 없으면 예약할 수 없다")
    void reserveOneThrowsExceptionWhenInventoryIsSoldOut() {
        // given
        var inventory = inventory(10L, 10L);
        given(inventoryRepository.getByProductIdWithLock(1L))
                .willReturn(Optional.of(inventory));

        // when
        var thrown = catchThrowable(() -> inventoryService.reserveOne(1L));

        // then
        assertThat(thrown)
                .isInstanceOf(SoldOutException.class);
    }

    @Test
    @DisplayName("재고가 있으면 판매 가능 여부를 true로 반환한다")
    void hasStockReturnsTrueWhenInventoryHasQuantity() {
        // given
        given(inventoryRepository.findByProductId(1L))
                .willReturn(Optional.of(inventory(10L, 9L)));

        // when
        var result = inventoryService.hasStock(1L);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("재고가 없으면 판매 가능 여부를 false로 반환한다")
    void hasStockReturnsFalseWhenInventoryDoesNotExist() {
        // given
        given(inventoryRepository.findByProductId(1L))
                .willReturn(Optional.empty());

        // when
        var result = inventoryService.hasStock(1L);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("판매 수량이 있으면 재고를 복구한다")
    void restoreOneDecreasesSoldQuantityWhenSoldQuantityExists() {
        // given
        var inventory = inventory(10L, 1L);
        given(inventoryRepository.getByProductIdWithLock(1L))
                .willReturn(Optional.of(inventory));

        // when
        inventoryService.restoreOne(1L);

        // then
        assertThat(inventory.getSoldQuantity()).isZero();
    }

    private Inventory inventory(long totalQuantity, long soldQuantity) {
        return Inventory.builder()
                .productId(1L)
                .totalQuantity(totalQuantity)
                .soldQuantity(soldQuantity)
                .build();
    }
}
