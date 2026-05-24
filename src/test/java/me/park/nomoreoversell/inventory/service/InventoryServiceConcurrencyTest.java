package me.park.nomoreoversell.inventory.service;

import me.park.nomoreoversell.TestcontainersConfiguration;
import me.park.nomoreoversell.exception.SoldOutException;
import me.park.nomoreoversell.inventory.domain.Inventory;
import me.park.nomoreoversell.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class InventoryServiceConcurrencyTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @BeforeEach
    void setUp() {
        inventoryRepository.deleteAllInBatch();
    }

    @AfterEach
    void tearDown() {
        inventoryRepository.deleteAllInBatch();
    }

    @Test
    @Timeout(10)
    @DisplayName("동시에 재고 예약을 요청해도 전체 수량보다 많이 성공하지 않는다")
    void reserveOneDoesNotOversellWhenConcurrentRequestsArrive() throws Exception {
        // given
        var productId = 1L;
        var requestCount = 1000;
        var totalQuantity = 10L;
        var expectedSuccessCount = 10;
        var expectedFailureCount = requestCount - expectedSuccessCount;

        inventoryRepository.saveAndFlush(Inventory.builder()
                .productId(productId)
                .totalQuantity(totalQuantity)
                .soldQuantity(0L)
                .build());

        var executor = Executors.newFixedThreadPool(requestCount);
        var readyLatch = new CountDownLatch(requestCount);
        var startLatch = new CountDownLatch(1);

        try {
            // when
            var futures = IntStream.range(0, requestCount)
                    .mapToObj(i -> executor.submit(() -> {
                        readyLatch.countDown();
                        startLatch.await();
                        try {
                            inventoryService.reserveOne(productId);
                            return true;
                        } catch (SoldOutException e) {
                            return false;
                        }
                    }))
                    .toList();

            assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
            startLatch.countDown();

            var successCount = 0;
            var failureCount = 0;
            for (var future : futures) {
                if (future.get(5, TimeUnit.SECONDS)) {
                    successCount++;
                } else {
                    failureCount++;
                }
            }

            var inventory = inventoryRepository.findByProductId(productId)
                    .orElseThrow();

            // then
            assertThat(successCount).isEqualTo(expectedSuccessCount);
            assertThat(failureCount).isEqualTo(expectedFailureCount);
            assertThat(inventory.getSoldQuantity()).isEqualTo(totalQuantity);
            assertThat(inventory.isSoldOut()).isTrue();
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }
}
