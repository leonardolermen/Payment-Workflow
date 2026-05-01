ALTER TABLE payments
    DROP CONSTRAINT IF EXISTS uc_payments_payeeid;

ALTER TABLE payments
    DROP CONSTRAINT IF EXISTS uc_payments_payerid;
