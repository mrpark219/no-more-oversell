package me.park.nomoreoversell.stayproduct.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class StayProductRepositoryCustomImpl implements StayProductRepositoryCustom {

    private final JPAQueryFactory factory;
}
