-- V1__init.sql
-- Baseline migration for core-service
-- Add your DDL scripts here (e.g., CREATE TABLE statements)
CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,

                       uuid UUID NOT NULL UNIQUE,

                       name VARCHAR(255) NOT NULL,

                       email VARCHAR(255) NOT NULL UNIQUE,

                       password VARCHAR(255) NOT NULL,

                       document VARCHAR(50) NOT NULL UNIQUE,

                       document_type VARCHAR(50) NOT NULL,

                       balance NUMERIC(19, 2) NOT NULL,

                       status VARCHAR(50) NOT NULL,

                       created_at TIMESTAMP NOT NULL
);