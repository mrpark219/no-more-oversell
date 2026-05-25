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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock
    private PointRepository pointRepository;

    @Mock
    private PointBalanceCache pointBalanceCache;

    @InjectMocks
    private PointService pointService;

    @Test
    @DisplayName("포인트가 존재하면 잔액을 반환한다")
    void getAvailableBalanceReturnsBalanceWhenPointExists() {
        // given
        given(pointBalanceCache.get(1L))
                .willReturn(Optional.empty());
        given(pointRepository.findByUserId(1L))
                .willReturn(Optional.of(point(10_000L)));

        // when
        var result = pointService.getAvailableBalance(1L);

        // then
        assertThat(result).isEqualTo(10_000L);
        verify(pointBalanceCache).put(1L, 10_000L);
    }

    @Test
    @DisplayName("포인트 캐시가 있으면 DB를 조회하지 않고 잔액을 반환한다")
    void getAvailableBalanceReturnsCachedBalanceWithoutRepositoryLookup() {
        // given
        given(pointBalanceCache.get(1L))
                .willReturn(Optional.of(10_000L));

        // when
        var result = pointService.getAvailableBalance(1L);

        // then
        assertThat(result).isEqualTo(10_000L);
        verifyNoInteractions(pointRepository);
    }

    @Test
    @DisplayName("포인트가 없으면 잔액을 0으로 반환한다")
    void getAvailableBalanceReturnsZeroWhenPointDoesNotExist() {
        // given
        given(pointBalanceCache.get(1L))
                .willReturn(Optional.empty());
        given(pointRepository.findByUserId(1L))
                .willReturn(Optional.empty());

        // when
        var result = pointService.getAvailableBalance(1L);

        // then
        assertThat(result).isZero();
        verify(pointBalanceCache).put(1L, 0L);
    }

    @Test
    @DisplayName("잔액이 충분하면 포인트를 차감한다")
    void deductBalanceDecreasesBalanceWhenBalanceIsEnough() {
        // given
        var point = point(10_000L);
        given(pointRepository.getByUserIdWithLock(1L))
                .willReturn(Optional.of(point));

        // when
        pointService.deductBalance(1L, 3_000L);

        // then
        assertThat(point.getBalance()).isEqualTo(7_000L);
        verify(pointBalanceCache).evict(1L);
    }

    @Test
    @DisplayName("포인트가 없으면 포인트를 차감할 수 없다")
    void deductBalanceThrowsExceptionWhenPointDoesNotExist() {
        // given
        given(pointRepository.getByUserIdWithLock(1L))
                .willReturn(Optional.empty());

        // when
        var thrown = catchThrowable(() -> pointService.deductBalance(1L, 3_000L));

        // then
        assertThat(thrown)
                .isInstanceOf(InsufficientPointBalanceException.class);
    }

    @Test
    @DisplayName("잔액이 부족하면 포인트 차감에 실패한다")
    void deductIfBalanceEnoughReturnsFalseWhenBalanceIsNotEnough() {
        // given
        var point = point(1_000L);
        given(pointRepository.getByUserIdWithLock(1L))
                .willReturn(Optional.of(point));

        // when
        var result = pointService.deductIfBalanceEnough(1L, 3_000L);

        // then
        assertThat(result).isFalse();
        assertThat(point.getBalance()).isEqualTo(1_000L);
        verify(pointBalanceCache).evict(1L);
    }

    @Test
    @DisplayName("포인트가 있으면 잔액을 복구한다")
    void restoreBalanceIncreasesBalanceWhenPointExists() {
        // given
        var point = point(7_000L);
        given(pointRepository.getByUserIdWithLock(1L))
                .willReturn(Optional.of(point));

        // when
        pointService.restoreBalance(1L, 3_000L);

        // then
        assertThat(point.getBalance()).isEqualTo(10_000L);
        verify(pointBalanceCache).evict(1L);
    }

    @Test
    @DisplayName("포인트가 없으면 복구할 수 없다")
    void restoreBalanceThrowsExceptionWhenPointDoesNotExist() {
        // given
        given(pointRepository.getByUserIdWithLock(1L))
                .willReturn(Optional.empty());

        // when
        var thrown = catchThrowable(() -> pointService.restoreBalance(1L, 3_000L));

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
