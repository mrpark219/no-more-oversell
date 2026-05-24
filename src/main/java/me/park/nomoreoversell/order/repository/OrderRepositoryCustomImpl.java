package me.park.nomoreoversell.order.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import me.park.nomoreoversell.order.domain.Order;
import me.park.nomoreoversell.order.domain.OrderStatus;

import java.util.Optional;

import static me.park.nomoreoversell.order.domain.QOrder.order;

@RequiredArgsConstructor
public class OrderRepositoryCustomImpl implements OrderRepositoryCustom {

    private final JPAQueryFactory factory;

    @Override
    public Optional<Order> findByOrderSheetId(Long orderSheetId) {
        return Optional.ofNullable(
                factory.selectFrom(order)
                        .where(order.orderSheetId.eq(orderSheetId))
                        .fetchOne()
        );
    }

    @Override
    public long countByUserIdAndProductIdAndStatus(Long userId, Long productId, OrderStatus status) {
        var count = factory.select(order.count())
                .from(order)
                .where(
                        order.userId.eq(userId),
                        order.productId.eq(productId),
                        order.status.eq(status)
                )
                .fetchOne();
        return count == null ? 0L : count;
    }
}
