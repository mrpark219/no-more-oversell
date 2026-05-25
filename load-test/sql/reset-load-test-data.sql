SET @product_id = 910001;
SET @base_user_id = 9100000;
SET @user_count = COALESCE(@load_test_user_count, 61000);

DELETE pd
FROM payment_detail pd
JOIN payment p ON pd.payment_id = p.id
JOIN order_sheet os ON p.order_sheet_id = os.id
WHERE os.product_id = @product_id;

DELETE p
FROM payment p
JOIN order_sheet os ON p.order_sheet_id = os.id
WHERE os.product_id = @product_id;

DELETE FROM orders
WHERE product_id = @product_id;

DELETE FROM order_sheet
WHERE product_id = @product_id;

DELETE FROM inventory
WHERE product_id = @product_id;

DELETE FROM stay_product
WHERE id = @product_id;

DELETE FROM point
WHERE user_id BETWEEN @base_user_id + 1 AND @base_user_id + @user_count;

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
VALUES (
    @product_id,
    '부하 테스트 한정 호텔',
    '스탠다드 더블',
    '10개 한정 특가',
    20000,
    10000,
    '2026-01-01 00:00:00.000000',
    'OPEN',
    '15:00:00.000000',
    '11:00:00.000000',
    1,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
);

INSERT INTO inventory (
    id,
    product_id,
    total_quantity,
    sold_quantity,
    created_at,
    updated_at
)
VALUES (
    @product_id,
    @product_id,
    10,
    0,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
);

INSERT INTO point (
    id,
    user_id,
    balance,
    created_at,
    updated_at
)
WITH digits(n) AS (
    SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
),
seq(n) AS (
    SELECT
        ones.n
        + tens.n * 10
        + hundreds.n * 100
        + thousands.n * 1000
        + ten_thousands.n * 10000
        + hundred_thousands.n * 100000
        + 1 AS n
    FROM digits ones
    CROSS JOIN digits tens
    CROSS JOIN digits hundreds
    CROSS JOIN digits thousands
    CROSS JOIN digits ten_thousands
    CROSS JOIN digits hundred_thousands
)
SELECT
    @base_user_id + n,
    @base_user_id + n,
    1000000,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
FROM seq
WHERE n <= @user_count;
