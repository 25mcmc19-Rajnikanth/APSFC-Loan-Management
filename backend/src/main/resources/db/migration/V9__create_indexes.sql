-- V9: additional composite indexes for the most common query access paths.
-- (Foreign-key columns are not implicitly indexed in PostgreSQL, so the FK
-- columns below are covered explicitly where they are on a hot query path.)

CREATE INDEX idx_users_status                    ON users (status);
CREATE INDEX idx_customers_city                  ON customers (city);

CREATE INDEX idx_applications_customer_status    ON loan_applications (customer_id, status);
CREATE INDEX idx_applications_status_product     ON loan_applications (status, product_id);
CREATE INDEX idx_applications_created_at         ON loan_applications (created_at);

CREATE INDEX idx_loans_status                    ON loans (status);
CREATE INDEX idx_loans_customer                  ON loans (customer_id);

CREATE INDEX idx_emi_loan_status                 ON emi_schedule (loan_id, payment_status);
CREATE INDEX idx_repayments_loan_date            ON repayments (loan_id, payment_date);
CREATE INDEX idx_repayments_txn_ref              ON repayments (transaction_ref);
CREATE INDEX idx_history_application             ON application_history (application_id);