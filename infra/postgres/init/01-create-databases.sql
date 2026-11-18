-- Family Finance Platform — PostgreSQL bootstrap
-- Chạy qua docker-entrypoint-initdb.d khi volume trống (lần đầu).
-- Mật khẩu placeholder — đồng bộ với .env / Spring datasource; không dùng production secrets trong repo.

CREATE USER profile_user WITH PASSWORD 'changeme_profile';
CREATE USER family_user WITH PASSWORD 'changeme_family';
CREATE USER wallet_user WITH PASSWORD 'changeme_wallet';
CREATE USER payment_user WITH PASSWORD 'changeme_payment';
CREATE USER budget_user WITH PASSWORD 'changeme_budget';
CREATE USER expense_user WITH PASSWORD 'changeme_expense';
CREATE USER household_user WITH PASSWORD 'changeme_household';
CREATE USER notification_user WITH PASSWORD 'changeme_notification';
CREATE USER analytics_user WITH PASSWORD 'changeme_analytics';

CREATE DATABASE profile_db OWNER profile_user;
CREATE DATABASE family_db OWNER family_user;
CREATE DATABASE wallet_db OWNER wallet_user;
CREATE DATABASE payment_db OWNER payment_user;
CREATE DATABASE budget_db OWNER budget_user;
CREATE DATABASE expense_db OWNER expense_user;
CREATE DATABASE household_db OWNER household_user;
CREATE DATABASE notification_db OWNER notification_user;
CREATE DATABASE analytics_db OWNER analytics_user;

\c profile_db
GRANT ALL ON SCHEMA public TO profile_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO profile_user;

\c family_db
GRANT ALL ON SCHEMA public TO family_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO family_user;

\c wallet_db
GRANT ALL ON SCHEMA public TO wallet_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO wallet_user;

\c payment_db
GRANT ALL ON SCHEMA public TO payment_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO payment_user;

\c budget_db
GRANT ALL ON SCHEMA public TO budget_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO budget_user;

\c expense_db
GRANT ALL ON SCHEMA public TO expense_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO expense_user;

\c household_db
GRANT ALL ON SCHEMA public TO household_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO household_user;

\c notification_db
GRANT ALL ON SCHEMA public TO notification_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO notification_user;

\c analytics_db
GRANT ALL ON SCHEMA public TO analytics_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO analytics_user;
