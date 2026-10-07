-- Drops every application object so Flyway can replay all migrations from scratch.
-- Run AS the "dime" role, against the database to reset:
--   psql -h 127.0.0.1 -U dime -d dime -f 01_reset_schema.sql
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
ALTER SCHEMA public OWNER TO current_user;
GRANT USAGE, CREATE ON SCHEMA public TO PUBLIC;
