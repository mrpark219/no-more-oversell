package me.park.nomoreoversell.payment.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import me.park.nomoreoversell.payment.domain.Payment;

import java.util.Optional;

import static me.park.nomoreoversell.payment.domain.QPayment.payment;

@RequiredArgsConstructor
public class PaymentRepositoryCustomImpl implements PaymentRepositoryCustom {

    private final JPAQueryFactory factory;

    @Override
    public Optional<Payment> findByOrderSheetId(Long orderSheetId) {
        return Optional.ofNullable(
                factory.selectFrom(payment)
                        .leftJoin(payment.details).fetchJoin()
                        .where(payment.orderSheetId.eq(orderSheetId))
                        .fetchOne()
        );
    }

    @Override
    public Optional<Payment> findByPaymentToken(String paymentToken) {
        return Optional.ofNullable(
                factory.selectFrom(payment)
                        .leftJoin(payment.details).fetchJoin()
                        .where(payment.paymentToken.eq(paymentToken))
                        .fetchOne()
        );
    }

    @Override
    public Optional<Payment> getByPaymentTokenWithLock(String paymentToken) {
        return Optional.ofNullable(
                factory.selectFrom(payment)
                        .leftJoin(payment.details).fetchJoin()
                        .where(payment.paymentToken.eq(paymentToken))
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .fetchOne()
        );
    }
}
