-- V5__fix_status_history.sql
-- Drop da tabela antiga e recriação com estrutura corrigida

DROP TABLE IF EXISTS status_history CASCADE;

CREATE TABLE status_history
(
    id             UUID DEFAULT gen_random_uuid() NOT NULL,
    payment_id     UUID                                    NOT NULL,
    old_status     VARCHAR(255)                            NOT NULL,
    new_status     VARCHAR(255)                            NOT NULL,
    source         VARCHAR(255)                            NOT NULL,
    change_reason  VARCHAR(500)                            NOT NULL,
    changed_by     VARCHAR(255)                            NOT NULL,
    timestamp      TIMESTAMP WITHOUT TIME ZONE             NOT NULL,
    CONSTRAINT pk_status_history PRIMARY KEY (id)
);

-- Índices para performance
CREATE INDEX idx_status_history_payment_id ON status_history(payment_id);
CREATE INDEX idx_status_history_timestamp ON status_history(timestamp);