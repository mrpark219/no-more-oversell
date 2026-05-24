CREATE TABLE order_sheet
(
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    order_sheet_token VARCHAR(100) NOT NULL,
    user_id           BIGINT       NOT NULL,
    product_id        BIGINT       NOT NULL,
    original_price    BIGINT       NOT NULL,
    sale_price        BIGINT       NOT NULL,
    status            VARCHAR(30)  NOT NULL,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY UNI_ORDER_SHEET_TOKEN (order_sheet_token),
    CONSTRAINT chk_order_sheet_price_non_negative CHECK (original_price >= 0 AND sale_price >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
