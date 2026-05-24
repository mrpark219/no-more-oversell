package me.park.nomoreoversell.payment.repository;

import me.park.nomoreoversell.payment.domain.Payment;

import java.util.Optional;

public interface PaymentRepositoryCustom {

    Optional<Payment> findByOrderSheetId(Long orderSheetId);

    Optional<Payment> findByPaymentToken(String paymentToken);

    Optional<Payment> getByPaymentTokenWithLock(String paymentToken);
}
