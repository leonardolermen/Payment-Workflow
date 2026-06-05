ALTER TABLE transactions
    ADD COLUMN payer_id UUID NOT NULL;

ALTER TABLE transactions
    ADD COLUMN payee_id UUID NOT NULL;

ALTER TABLE transactions
    ALTER COLUMN payment_id TYPE UUID USING payment_id::uuid;