package me.park.nomoreoversell.stayproduct.repository;

import me.park.nomoreoversell.stayproduct.domain.StayProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StayProductRepository extends JpaRepository<StayProduct, Long>, StayProductRepositoryCustom {
}
