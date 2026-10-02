-- V12: add updated_at to repayments so the ledger table matches the shared
-- BaseAuditableEntity mapping (uniform audit columns across all tables).

ALTER TABLE repayments
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;