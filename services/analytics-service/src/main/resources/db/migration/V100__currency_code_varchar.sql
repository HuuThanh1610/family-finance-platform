-- Align currency_code columns with JPA VARCHAR mapping.
ALTER TABLE family_monthly_summary ALTER COLUMN currency_code TYPE VARCHAR(3);
ALTER TABLE member_spending_summary ALTER COLUMN currency_code TYPE VARCHAR(3);
ALTER TABLE budget_category_actual ALTER COLUMN currency_code TYPE VARCHAR(3);
ALTER TABLE wallet_balance_summary ALTER COLUMN currency_code TYPE VARCHAR(3);
