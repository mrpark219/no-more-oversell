package me.park.nomoreoversell.order.repository;

import me.park.nomoreoversell.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long>, OrderRepositoryCustom {
}
