-- Revierte 002_manual_entries_and_budget.sql. Simétrico a drop.sql (mismo
-- patrón: útil mientras se itera, nunca correrlo contra datos que importan).

DROP TABLE category_budget_targets CASCADE CONSTRAINTS PURGE;

ALTER TABLE transactions DROP CONSTRAINT fk_transactions_linked;
ALTER TABLE transactions DROP CONSTRAINT ck_transactions_category_confidence;
ALTER TABLE transactions DROP CONSTRAINT ck_transactions_category_method;
ALTER TABLE transactions DROP CONSTRAINT ck_transactions_link_status;
ALTER TABLE transactions DROP CONSTRAINT ck_transactions_payment_method;
ALTER TABLE transactions DROP CONSTRAINT ck_transactions_source;
ALTER TABLE transactions DROP COLUMN category_confidence;
ALTER TABLE transactions DROP COLUMN category_method;
ALTER TABLE transactions DROP COLUMN linked_transaction_id;
ALTER TABLE transactions DROP COLUMN link_status;
ALTER TABLE transactions DROP COLUMN payment_method;
ALTER TABLE transactions DROP COLUMN source;
ALTER TABLE transactions MODIFY (category NOT NULL);
