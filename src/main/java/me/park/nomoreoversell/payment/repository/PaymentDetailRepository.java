package me.park.nomoreoversell.payment.repository;

import me.park.nomoreoversell.payment.domain.PaymentDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentDetailRepository extends JpaRepository<PaymentDetail, Long> {
}
