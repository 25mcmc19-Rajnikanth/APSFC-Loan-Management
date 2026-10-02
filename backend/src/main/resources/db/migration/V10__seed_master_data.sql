-- V10: seed master data - configurable loan products (reference data, not
-- application data). Staff/administrator accounts are provisioned in code.

INSERT INTO loan_products
    (name, code, description, min_amount, max_amount, base_interest_rate,
     min_tenure_months, max_tenure_months, eligibility_criteria, status)
VALUES
    ('Personal Loan',      'PERSONAL_LOAN', NULL, 50000.00, 2000000.00, 11.50, 12, 60,  'Salaried / self-employed, min 21 yrs', 'ACTIVE'),
    ('Business Loan',      'BUSINESS_LOAN', NULL, 200000.00, 10000000.00, 13.00, 24, 120, 'Business vintage min 3 years',        'ACTIVE'),
    ('Vehicle Loan',       'VEHICLE_LOAN',  NULL, 100000.00, 5000000.00, 10.50, 12, 84,  'New/used 2 or 4 wheeler',             'ACTIVE'),
    ('Education Loan',     'EDUCATION_LOAN', NULL, 50000.00, 1500000.00,  9.50, 12, 96,  'Admission confirmation required',     'ACTIVE'),
    ('Housing Loan',       'HOUSING_LOAN',  NULL, 500000.00, 15000000.00,  8.80, 12, 360, 'Property documents via bank empanelment','ACTIVE');