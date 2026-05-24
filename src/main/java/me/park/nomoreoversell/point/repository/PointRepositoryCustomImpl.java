package me.park.nomoreoversell.point.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import me.park.nomoreoversell.point.domain.Point;

import java.util.Optional;

import static me.park.nomoreoversell.point.domain.QPoint.point;

@RequiredArgsConstructor
public class PointRepositoryCustomImpl implements PointRepositoryCustom {

    private final JPAQueryFactory factory;

    @Override
    public Optional<Point> getByUserIdWithLock(Long userId) {
        return Optional.ofNullable(
                factory.selectFrom(point)
                        .where(point.userId.eq(userId))
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .fetchOne()
        );
    }
}
