-- Align currency_code with JPA String/VARCHAR mapping (CHAR caused schema-validation failures).
ALTER TABLE wallet_accounts
    ALTER COLUMN currency_code TYPE VARCHAR(3);
