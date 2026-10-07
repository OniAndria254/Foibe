-- Idempotent. Must be run as a superuser:
--   psql -h 127.0.0.1 -U postgres -v app_password="<password>" -f 00_create_role_and_databases.sql
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', 'dime', :'app_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'dime')
\gexec

ALTER ROLE dime LOGIN PASSWORD :'app_password';

SELECT format('CREATE DATABASE %I OWNER %I', 'dime', 'dime')
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'dime')
\gexec

SELECT format('CREATE DATABASE %I OWNER %I', 'dime_test', 'dime')
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'dime_test')
\gexec

ALTER DATABASE dime OWNER TO dime;
ALTER DATABASE dime_test OWNER TO dime;

GRANT CONNECT, CREATE ON DATABASE dime TO dime;
GRANT CONNECT, CREATE ON DATABASE dime_test TO dime;
