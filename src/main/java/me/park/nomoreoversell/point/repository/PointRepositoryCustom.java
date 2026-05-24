package me.park.nomoreoversell.point.repository;

import me.park.nomoreoversell.point.domain.Point;

import java.util.Optional;

public interface PointRepositoryCustom {

    Optional<Point> getByUserIdWithLock(Long userId);
}
