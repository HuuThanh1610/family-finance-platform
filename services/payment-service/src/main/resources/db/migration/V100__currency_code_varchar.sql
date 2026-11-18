-- Align currency_code with JPA String/VARCHAR mapping.
ALTER TABLE payment_orders
    ALTER COLUMN currency_code TYPE VARCHAR(3);
