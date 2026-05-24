package me.park.nomoreoversell.order.repository;

import me.park.nomoreoversell.order.domain.Order;
import me.park.nomoreoversell.order.domain.OrderStatus;

import java.util.Optional;

public interface OrderRepositoryCustom {

    Optional<Order> findByOrderSheetId(Long orderSheetId);

    long countByUserIdAndProductIdAndStatus(Long userId, Long productId, OrderStatus status);
}
