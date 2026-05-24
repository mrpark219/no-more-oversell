CREATE TABLE inventory
(
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    product_id    BIGINT      NOT NULL,
    total_quantity BIGINT     NOT NULL,
    sold_quantity  BIGINT     NOT NULL,
    created_at    DATETIME(6) NOT NULL,
    updated_at    DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY UNI_INVENTORY_PRODUCT_ID (product_id),
    CONSTRAINT CHK_INVENTORY_TOTAL_QUANTITY_NON_NEGATIVE CHECK (total_quantity >= 0),
    CONSTRAINT CHK_INVENTORY_SOLD_QUANTITY_RANGE CHECK (sold_quantity >= 0 AND sold_quantity <= total_quantity)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
