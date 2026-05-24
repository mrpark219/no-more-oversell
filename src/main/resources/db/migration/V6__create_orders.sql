CREATE TABLE orders
(
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    order_sheet_id BIGINT      NOT NULL,
    user_id        BIGINT      NOT NULL,
    product_id     BIGINT      NOT NULL,
    original_price BIGINT      NOT NULL,
    sale_price     BIGINT      NOT NULL,
    status         VARCHAR(30) NOT NULL,
    created_at     DATETIME(6) NOT NULL,
    updated_at     DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY UNI_ORDERS_ORDER_SHEET_ID (order_sheet_id),
    KEY IDX_ORDERS_USER_PRODUCT_STATUS (user_id, product_id, status),
    CONSTRAINT CHK_ORDERS_PRICE_NON_NEGATIVE CHECK (original_price >= 0 AND sale_price >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
