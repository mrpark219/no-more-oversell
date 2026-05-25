package me.park.nomoreoversell.ordersheet.repository;

import me.park.nomoreoversell.ordersheet.domain.OrderSheet;

import java.util.Optional;

public interface OrderSheetRepositoryCustom {

    Optional<OrderSheet> findByToken(String orderSheetToken);

    Optional<OrderSheet> getByTokenWithLock(String orderSheetToken);

    Optional<OrderSheet> getByIdWithLock(Long id);
}
