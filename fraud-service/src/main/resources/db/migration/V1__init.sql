-- V1__init.sql
-- Baseline migration for fraud-service
-- Add your DDL scripts here (e.g., CREATE TABLE statements)
CREATE TABLE fraud_analysis_logs (
                                     id BIGSERIAL PRIMARY KEY,

                                     uuid UUID NOT NULL UNIQUE,
                                     payment_id UUID NOT NULL,

                                     score DOUBLE PRECISION NOT NULL,

                                     status VARCHAR(50) NOT NULL,

                                     reason VARCHAR(255) NOT NULL,

                                     evaluated_at TIMESTAMP NOT NULL
);