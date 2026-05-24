package me.park.nomoreoversell.point.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.park.nomoreoversell.exception.InsufficientPointBalanceException;
import me.park.nomoreoversell.exception.PointNotFoundException;
import me.park.nomoreoversell.point.domain.Point;
import me.park.nomoreoversell.point.repository.PointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointService {

    private final PointRepository pointRepository;

    @Transactional(readOnly = true)
    public long available(Long userId) {
        log.debug("포인트 잔액 조회 시도. userId={}", userId);

        var balance = pointRepository.findByUserId(userId)
                .map(Point::getBalance)
                .orElse(0L);

        log.debug("포인트 잔액 조회 완료. userId={}, balance={}", userId, balance);
        return balance;
    }

    @Transactional
    public void deduct(Long userId, long amount) {
        log.info("포인트 차감 시도. userId={}, amount={}", userId, amount);

        var point = pointRepository.getByUserIdWithLock(userId)
                .orElseThrow(() -> {
                    log.warn("포인트 차감 실패: 포인트가 존재하지 않습니다. userId={}, amount={}", userId, amount);
                    return new InsufficientPointBalanceException();
                });

        point.deduct(amount);
        log.info("포인트 차감 성공. userId={}, amount={}, balance={}", userId, amount, point.getBalance());
    }

    @Transactional
    public boolean deductIfEnough(Long userId, long amount) {
        log.info("포인트 차감 시도. userId={}, amount={}", userId, amount);

        var point = pointRepository.getByUserIdWithLock(userId)
                .orElse(null);

        if (point == null) {
            log.warn("포인트 차감 실패: 포인트가 존재하지 않습니다. userId={}, amount={}", userId, amount);
            return false;
        }

        if (!point.canAfford(amount)) {
            log.info(
                    "포인트 차감 실패: 잔액이 부족합니다. userId={}, amount={}, balance={}",
                    userId,
                    amount,
                    point.getBalance()
            );
            return false;
        }

        point.deduct(amount);

        log.info("포인트 차감 성공. userId={}, amount={}, balance={}", userId, amount, point.getBalance());
        return true;
    }

    @Transactional
    public void restore(Long userId, long amount) {
        log.info("포인트 복구 시도. userId={}, amount={}", userId, amount);

        var point = pointRepository.getByUserIdWithLock(userId)
                .orElseThrow(() -> {
                    log.warn("포인트 복구 실패: 포인트가 존재하지 않습니다. userId={}, amount={}", userId, amount);
                    return new PointNotFoundException();
                });

        point.restore(amount);
        log.info("포인트 복구 성공. userId={}, amount={}, balance={}", userId, amount, point.getBalance());
    }
}
