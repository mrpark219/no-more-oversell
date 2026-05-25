ALTER TABLE order_sheet
    ADD COLUMN failure_reason VARCHAR(50) NULL AFTER status;
