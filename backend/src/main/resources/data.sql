-- Seed data for the dev (H2) profile only; spring.sql.init.mode=never on Oracle,
-- where db/oracle/data.sql does the same job.
INSERT INTO currencies (name_currency) VALUES ('CLP');
INSERT INTO currencies (name_currency) VALUES ('USD');
INSERT INTO currencies (name_currency) VALUES ('EUR');
