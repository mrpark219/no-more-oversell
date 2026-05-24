INSERT INTO stay_product (
    id,
    accommodation_name,
    room_name,
    rate_plan_name,
    original_price,
    sale_price,
    open_at,
    status,
    checkin_time,
    checkout_time,
    max_per_user,
    created_at,
    updated_at
)
VALUES
    (
        900001,
        '로컬 테스트 호텔',
        '디럭스 더블',
        '선착순 특가',
        20000,
        10000,
        '2026-01-01 00:00:00.000000',
        'OPEN',
        '15:00:00.000000',
        '11:00:00.000000',
        1,
        CURRENT_TIMESTAMP(6),
        CURRENT_TIMESTAMP(6)
    ),
    (
        900002,
        '로컬 품절 호텔',
        '스탠다드 더블',
        '품절 확인용',
        15000,
        5000,
        '2026-01-01 00:00:00.000000',
        'OPEN',
        '15:00:00.000000',
        '11:00:00.000000',
        1,
        CURRENT_TIMESTAMP(6),
        CURRENT_TIMESTAMP(6)
    ) AS seed
ON DUPLICATE KEY UPDATE
    accommodation_name = seed.accommodation_name,
    room_name = seed.room_name,
    rate_plan_name = seed.rate_plan_name,
    original_price = seed.original_price,
    sale_price = seed.sale_price,
    open_at = seed.open_at,
    status = seed.status,
    checkin_time = seed.checkin_time,
    checkout_time = seed.checkout_time,
    max_per_user = seed.max_per_user,
    updated_at = seed.updated_at;

INSERT INTO inventory (
    id,
    product_id,
    total_quantity,
    sold_quantity,
    created_at,
    updated_at
)
VALUES
    (
        900001,
        900001,
        10,
        0,
        CURRENT_TIMESTAMP(6),
        CURRENT_TIMESTAMP(6)
    ),
    (
        900002,
        900002,
        1,
        1,
        CURRENT_TIMESTAMP(6),
        CURRENT_TIMESTAMP(6)
    ) AS seed
ON DUPLICATE KEY UPDATE
    product_id = seed.product_id,
    total_quantity = seed.total_quantity,
    sold_quantity = seed.sold_quantity,
    updated_at = seed.updated_at;

INSERT INTO point (
    id,
    user_id,
    balance,
    created_at,
    updated_at
)
VALUES
    (
        900001,
        900001,
        100000,
        CURRENT_TIMESTAMP(6),
        CURRENT_TIMESTAMP(6)
    ),
    (
        900002,
        900002,
        0,
        CURRENT_TIMESTAMP(6),
        CURRENT_TIMESTAMP(6)
    ) AS seed
ON DUPLICATE KEY UPDATE
    user_id = seed.user_id,
    balance = seed.balance,
    updated_at = seed.updated_at;
