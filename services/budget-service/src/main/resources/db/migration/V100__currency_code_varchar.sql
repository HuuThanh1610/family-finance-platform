-- Align currency_code with JPA VARCHAR mapping.
ALTER TABLE budgets ALTER COLUMN currency_code TYPE VARCHAR(3);
