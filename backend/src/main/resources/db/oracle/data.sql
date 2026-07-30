-- Reference data for Oracle. Run after schema.sql, as the application schema user.
-- Transactions reference a currency, so at least one row has to exist here.
INSERT INTO currencies (name_currency) VALUES ('CLP');
INSERT INTO currencies (name_currency) VALUES ('USD');
INSERT INTO currencies (name_currency) VALUES ('EUR');
COMMIT;
