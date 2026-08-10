-- Revierte 003_financial_profile_history.sql. Mismo patrón que drop.sql y
-- 002_manual_entries_and_budget_drop.sql: útil mientras se itera, nunca
-- correrlo contra datos que importan.

DROP TABLE financial_profile_history CASCADE CONSTRAINTS PURGE;
