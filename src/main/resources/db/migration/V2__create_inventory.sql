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
    CONSTRAINT chk_inventory_total_quantity_non_negative CHECK (total_quantity >= 0),
    CONSTRAINT chk_inventory_sold_quantity_range CHECK (sold_quantity >= 0 AND sold_quantity <= total_quantity)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
