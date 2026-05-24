CREATE TABLE payment
(
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    order_sheet_id BIGINT       NOT NULL,
    payment_token  VARCHAR(100) NOT NULL,
    status         VARCHAR(30)  NOT NULL,
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY UNI_PAYMENT_ORDER_SHEET_ID (order_sheet_id),
    UNIQUE KEY UNI_PAYMENT_TOKEN (payment_token)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE payment_detail
(
    id                       BIGINT      NOT NULL AUTO_INCREMENT,
    payment_id               BIGINT      NOT NULL,
    payment_method           VARCHAR(20) NOT NULL,
    payment_amount           BIGINT      NOT NULL,
    canceled_amount          BIGINT      NOT NULL,
    remaining_amount         BIGINT      NOT NULL,
    external_transaction_key VARCHAR(100) NULL,
    cancel_key               VARCHAR(100) NULL,
    status                   VARCHAR(30) NOT NULL,
    failure_reason           VARCHAR(255) NULL,
    approved_at              DATETIME(6) NULL,
    canceled_at              DATETIME(6) NULL,
    created_at               DATETIME(6) NOT NULL,
    updated_at               DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY UNI_PAYMENT_DETAIL_PAYMENT_METHOD (payment_id, payment_method),
    UNIQUE KEY UNI_PAYMENT_DETAIL_EXTERNAL_TRANSACTION_KEY (payment_id, external_transaction_key),
    CONSTRAINT fk_payment_detail_payment FOREIGN KEY (payment_id) REFERENCES payment (id),
    CONSTRAINT CHK_PAYMENT_DETAIL_PAYMENT_AMOUNT_NON_NEGATIVE CHECK (payment_amount >= 0),
    CONSTRAINT CHK_PAYMENT_DETAIL_CANCELED_AMOUNT_NON_NEGATIVE CHECK (canceled_amount >= 0),
    CONSTRAINT CHK_PAYMENT_DETAIL_REMAINING_AMOUNT_NON_NEGATIVE CHECK (remaining_amount >= 0),
    CONSTRAINT CHK_PAYMENT_DETAIL_AMOUNT_CONSISTENCY CHECK (payment_amount = canceled_amount + remaining_amount)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
