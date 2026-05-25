package me.park.nomoreoversell.ordersheet.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import me.park.nomoreoversell.ordersheet.domain.OrderSheet;

import java.util.Optional;

import static me.park.nomoreoversell.ordersheet.domain.QOrderSheet.orderSheet;

@RequiredArgsConstructor
public class OrderSheetRepositoryCustomImpl implements OrderSheetRepositoryCustom {

    private final JPAQueryFactory factory;

    @Override
    public Optional<OrderSheet> findByToken(String orderSheetToken) {
        return Optional.ofNullable(
                factory.selectFrom(orderSheet)
                        .where(orderSheet.orderSheetToken.eq(orderSheetToken))
                        .fetchOne()
        );
    }

    @Override
    public Optional<OrderSheet> getByTokenWithLock(String orderSheetToken) {
        return Optional.ofNullable(
                factory.selectFrom(orderSheet)
                        .where(orderSheet.orderSheetToken.eq(orderSheetToken))
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .fetchOne()
        );
    }

    @Override
    public Optional<OrderSheet> getByIdWithLock(Long id) {
        return Optional.ofNullable(
                factory.selectFrom(orderSheet)
                        .where(orderSheet.id.eq(id))
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .fetchOne()
        );
    }
}
