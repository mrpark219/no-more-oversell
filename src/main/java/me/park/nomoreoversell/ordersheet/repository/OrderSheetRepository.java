package me.park.nomoreoversell.ordersheet.repository;

import me.park.nomoreoversell.ordersheet.domain.OrderSheet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderSheetRepository extends JpaRepository<OrderSheet, Long>, OrderSheetRepositoryCustom {
}
