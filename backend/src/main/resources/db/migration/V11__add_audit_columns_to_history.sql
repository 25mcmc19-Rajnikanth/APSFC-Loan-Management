-- V11: add standard audit columns to application_history (entity inherits
-- BaseAuditableEntity; column set must match).

ALTER TABLE application_history
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;