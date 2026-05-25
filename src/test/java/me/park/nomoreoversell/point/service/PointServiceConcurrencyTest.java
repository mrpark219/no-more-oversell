package me.park.nomoreoversell.point.service;

import me.park.nomoreoversell.TestcontainersConfiguration;
import me.park.nomoreoversell.point.domain.Point;
import me.park.nomoreoversell.point.repository.PointRepository;
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
class PointServiceConcurrencyTest {

    @Autowired
    private PointService pointService;

    @Autowired
    private PointRepository pointRepository;

    @BeforeEach
    void setUp() {
        pointRepository.deleteAllInBatch();
    }

    @AfterEach
    void tearDown() {
        pointRepository.deleteAllInBatch();
    }

    @Test
    @Timeout(10)
    @DisplayName("동시에 포인트 차감을 요청해도 잔액보다 많이 성공하지 않는다")
    void deductIfBalanceEnoughDoesNotOverDeductWhenConcurrentRequestsArrive() throws Exception {
        // given
        var userId = 1L;
        var requestCount = 1000;
        var deductAmount = 1_000L;
        var initialBalance = 10_000L;
        var expectedSuccessCount = 10;
        var expectedFailureCount = requestCount - expectedSuccessCount;

        pointRepository.saveAndFlush(Point.builder()
                .userId(userId)
                .balance(initialBalance)
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
                        return pointService.deductIfBalanceEnough(userId, deductAmount);
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

            var point = pointRepository.findByUserId(userId)
                    .orElseThrow();

            // then
            assertThat(successCount).isEqualTo(expectedSuccessCount);
            assertThat(failureCount).isEqualTo(expectedFailureCount);
            assertThat(point.getBalance()).isZero();
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }
}
