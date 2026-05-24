package me.park.nomoreoversell.point.service;

import me.park.nomoreoversell.exception.InsufficientPointBalanceException;
import me.park.nomoreoversell.exception.PointNotFoundException;
import me.park.nomoreoversell.point.domain.Point;
import me.park.nomoreoversell.point.repository.PointRepository;
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
class PointServiceTest {

    @Mock
    private PointRepository pointRepository;

    @InjectMocks
    private PointService pointService;

    @Test
    @DisplayName("포인트가 존재하면 잔액을 반환한다")
    void availableReturnsBalanceWhenPointExists() {
        // given
        given(pointRepository.findByUserId(1L))
                .willReturn(Optional.of(point(10_000L)));

        // when
        var result = pointService.available(1L);

        // then
        assertThat(result).isEqualTo(10_000L);
    }

    @Test
    @DisplayName("포인트가 없으면 잔액을 0으로 반환한다")
    void availableReturnsZeroWhenPointDoesNotExist() {
        // given
        given(pointRepository.findByUserId(1L))
                .willReturn(Optional.empty());

        // when
        var result = pointService.available(1L);

        // then
        assertThat(result).isZero();
    }

    @Test
    @DisplayName("잔액이 충분하면 포인트를 차감한다")
    void deductDecreasesBalanceWhenBalanceIsEnough() {
        // given
        var point = point(10_000L);
        given(pointRepository.getByUserIdWithLock(1L))
                .willReturn(Optional.of(point));

        // when
        pointService.deduct(1L, 3_000L);

        // then
        assertThat(point.getBalance()).isEqualTo(7_000L);
    }

    @Test
    @DisplayName("포인트가 없으면 포인트를 차감할 수 없다")
    void deductThrowsExceptionWhenPointDoesNotExist() {
        // given
        given(pointRepository.getByUserIdWithLock(1L))
                .willReturn(Optional.empty());

        // when
        var thrown = catchThrowable(() -> pointService.deduct(1L, 3_000L));

        // then
        assertThat(thrown)
                .isInstanceOf(InsufficientPointBalanceException.class);
    }

    @Test
    @DisplayName("잔액이 부족하면 포인트 차감에 실패한다")
    void deductIfEnoughReturnsFalseWhenBalanceIsNotEnough() {
        // given
        var point = point(1_000L);
        given(pointRepository.getByUserIdWithLock(1L))
                .willReturn(Optional.of(point));

        // when
        var result = pointService.deductIfEnough(1L, 3_000L);

        // then
        assertThat(result).isFalse();
        assertThat(point.getBalance()).isEqualTo(1_000L);
    }

    @Test
    @DisplayName("포인트가 있으면 잔액을 복구한다")
    void restoreIncreasesBalanceWhenPointExists() {
        // given
        var point = point(7_000L);
        given(pointRepository.getByUserIdWithLock(1L))
                .willReturn(Optional.of(point));

        // when
        pointService.restore(1L, 3_000L);

        // then
        assertThat(point.getBalance()).isEqualTo(10_000L);
    }

    @Test
    @DisplayName("포인트가 없으면 복구할 수 없다")
    void restoreThrowsExceptionWhenPointDoesNotExist() {
        // given
        given(pointRepository.getByUserIdWithLock(1L))
                .willReturn(Optional.empty());

        // when
        var thrown = catchThrowable(() -> pointService.restore(1L, 3_000L));

        // then
        assertThat(thrown)
                .isInstanceOf(PointNotFoundException.class);
    }

    private Point point(long balance) {
        return Point.builder()
                .userId(1L)
                .balance(balance)
                .build();
    }
}
