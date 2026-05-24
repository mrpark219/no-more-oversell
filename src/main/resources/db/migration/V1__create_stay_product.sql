CREATE TABLE stay_product
(
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    accommodation_name VARCHAR(255) NOT NULL,
    room_name          VARCHAR(255) NOT NULL,
    rate_plan_name     VARCHAR(255) NOT NULL,
    original_price     BIGINT       NOT NULL,
    sale_price         BIGINT       NOT NULL,
    open_at            DATETIME(6)  NOT NULL,
    status             VARCHAR(30)  NOT NULL,
    checkin_time       TIME(6)      NOT NULL,
    checkout_time      TIME(6)      NOT NULL,
    max_per_user       BIGINT       NULL,
    created_at         DATETIME(6)  NOT NULL,
    updated_at         DATETIME(6)  NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
