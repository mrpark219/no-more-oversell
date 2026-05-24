package me.park.nomoreoversell.payment.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PaymentDetailRepositoryCustomImpl implements PaymentDetailRepositoryCustom {

    private final JPAQueryFactory factory;

}
