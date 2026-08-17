-- Seed data for the dev (H2) profile only; spring.sql.init.mode=never on Oracle,
-- where db/oracle/data.sql does the same job.
INSERT INTO currencies (name_currency) VALUES ('CLP');
INSERT INTO currencies (name_currency) VALUES ('USD');
INSERT INTO currencies (name_currency) VALUES ('EUR');

-- Seeding de palabras clave para clasificación de transacciones (Nivel 2)
INSERT INTO category_keywords (keyword, category) VALUES ('SUPERMERCADO', 'FOOD');
INSERT INTO category_keywords (keyword, category) VALUES ('LIDER', 'FOOD');
INSERT INTO category_keywords (keyword, category) VALUES ('JUMBO', 'FOOD');
INSERT INTO category_keywords (keyword, category) VALUES ('TOTTUS', 'FOOD');
INSERT INTO category_keywords (keyword, category) VALUES ('UNIMARC', 'FOOD');
INSERT INTO category_keywords (keyword, category) VALUES ('ALIMENTACION', 'FOOD');
INSERT INTO category_keywords (keyword, category) VALUES ('RESTAURANT', 'FOOD');
INSERT INTO category_keywords (keyword, category) VALUES ('UBER', 'TRANSPORT');
INSERT INTO category_keywords (keyword, category) VALUES ('CABIFY', 'TRANSPORT');
INSERT INTO category_keywords (keyword, category) VALUES ('DIDI', 'TRANSPORT');
INSERT INTO category_keywords (keyword, category) VALUES ('METRO', 'TRANSPORT');
INSERT INTO category_keywords (keyword, category) VALUES ('BIP', 'TRANSPORT');
INSERT INTO category_keywords (keyword, category) VALUES ('COPEC', 'TRANSPORT');
INSERT INTO category_keywords (keyword, category) VALUES ('BENCINA', 'TRANSPORT');
INSERT INTO category_keywords (keyword, category) VALUES ('VIVIENDA', 'HOUSING');
INSERT INTO category_keywords (keyword, category) VALUES ('DEPARTAMENTO', 'HOUSING');
INSERT INTO category_keywords (keyword, category) VALUES ('ARRIENDO', 'HOUSING');
INSERT INTO category_keywords (keyword, category) VALUES ('DIVIDENDO', 'HOUSING');
INSERT INTO category_keywords (keyword, category) VALUES ('CASA', 'HOUSING');
INSERT INTO category_keywords (keyword, category) VALUES ('AGUA', 'UTILITIES');
INSERT INTO category_keywords (keyword, category) VALUES ('LUZ', 'UTILITIES');
INSERT INTO category_keywords (keyword, category) VALUES ('GAS', 'UTILITIES');
INSERT INTO category_keywords (keyword, category) VALUES ('INTERNET', 'UTILITIES');
INSERT INTO category_keywords (keyword, category) VALUES ('NETFLIX', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('SPOTIFY', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('CINE', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('STREAMING', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('FARMACIA', 'HEALTH');
INSERT INTO category_keywords (keyword, category) VALUES ('CLINICA', 'HEALTH');
INSERT INTO category_keywords (keyword, category) VALUES ('MEDICO', 'HEALTH');
INSERT INTO category_keywords (keyword, category) VALUES ('COLEGIO', 'EDUCATION');
INSERT INTO category_keywords (keyword, category) VALUES ('UNIVERSIDAD', 'EDUCATION');
INSERT INTO category_keywords (keyword, category) VALUES ('COMPRA', 'SHOPPING');
INSERT INTO category_keywords (keyword, category) VALUES ('MALL', 'SHOPPING');
INSERT INTO category_keywords (keyword, category) VALUES ('SUELDO', 'SALARY');
INSERT INTO category_keywords (keyword, category) VALUES ('REMUNERACION', 'SALARY');

-- ADICIONES PARA EVITAR FALLBACKS (Chile / LATAM)
-- VIVIENDA (HOUSING)
INSERT INTO category_keywords (keyword, category) VALUES ('CONDOMINIO', 'HOUSING');
INSERT INTO category_keywords (keyword, category) VALUES ('CORREDOR', 'HOUSING');
INSERT INTO category_keywords (keyword, category) VALUES ('PAGO DEPARTAMENTO', 'HOUSING');
INSERT INTO category_keywords (keyword, category) VALUES ('EDIFICIO', 'HOUSING');
INSERT INTO category_keywords (keyword, category) VALUES ('MUDANZA', 'HOUSING');

-- SERVICIOS BASICOS (UTILITIES)
INSERT INTO category_keywords (keyword, category) VALUES ('SERVIPAG', 'UTILITIES');
INSERT INTO category_keywords (keyword, category) VALUES ('SENCILLITO', 'UTILITIES');
INSERT INTO category_keywords (keyword, category) VALUES ('UNIRED', 'UTILITIES');
INSERT INTO category_keywords (keyword, category) VALUES ('DIRECTV', 'UTILITIES');
INSERT INTO category_keywords (keyword, category) VALUES ('MOVISTAR HOGAR', 'UTILITIES');
INSERT INTO category_keywords (keyword, category) VALUES ('TELEFONICA', 'UTILITIES');
INSERT INTO category_keywords (keyword, category) VALUES ('MOVIL', 'UTILITIES');

-- ALIMENTACIÓN (FOOD)
INSERT INTO category_keywords (keyword, category) VALUES ('OXXO', 'FOOD');
INSERT INTO category_keywords (keyword, category) VALUES ('OK MARKET', 'FOOD');
INSERT INTO category_keywords (keyword, category) VALUES ('DOGGIS', 'FOOD');
INSERT INTO category_keywords (keyword, category) VALUES ('WENDYS', 'FOOD');
INSERT INTO category_keywords (keyword, category) VALUES ('SANGUCHERIA', 'FOOD');
INSERT INTO category_keywords (keyword, category) VALUES ('FUENTEMAYOR', 'FOOD');
INSERT INTO category_keywords (keyword, category) VALUES ('SUSHI', 'FOOD');
INSERT INTO category_keywords (keyword, category) VALUES ('DELIVERY', 'FOOD');

-- TRANSPORTE (TRANSPORT)
INSERT INTO category_keywords (keyword, category) VALUES ('TAG', 'TRANSPORT');
INSERT INTO category_keywords (keyword, category) VALUES ('AUTOPISTA CENTRAL', 'TRANSPORT');
INSERT INTO category_keywords (keyword, category) VALUES ('PRT', 'TRANSPORT');
INSERT INTO category_keywords (keyword, category) VALUES ('COLECTIVO', 'TRANSPORT');
INSERT INTO category_keywords (keyword, category) VALUES ('TRANSANTIAGO', 'TRANSPORT');
INSERT INTO category_keywords (keyword, category) VALUES ('TREN', 'TRANSPORT');
INSERT INTO category_keywords (keyword, category) VALUES ('EFE', 'TRANSPORT');

-- SALUD (HEALTH)
INSERT INTO category_keywords (keyword, category) VALUES ('CONSALUD', 'HEALTH');
INSERT INTO category_keywords (keyword, category) VALUES ('VIDA TRES', 'HEALTH');
INSERT INTO category_keywords (keyword, category) VALUES ('MEDS', 'HEALTH');
INSERT INTO category_keywords (keyword, category) VALUES ('ACHS', 'HEALTH');
INSERT INTO category_keywords (keyword, category) VALUES ('MUTUAL', 'HEALTH');
INSERT INTO category_keywords (keyword, category) VALUES ('DOCTOR SIMI', 'HEALTH');
INSERT INTO category_keywords (keyword, category) VALUES ('FARMACIA SIMI', 'HEALTH');

-- EDUCACIÓN (EDUCATION)
INSERT INTO category_keywords (keyword, category) VALUES ('OPEN ENGLISH', 'EDUCATION');
INSERT INTO category_keywords (keyword, category) VALUES ('DUOLINGO', 'EDUCATION');

-- ENTRETENIMIENTO (ENTERTAINMENT)
INSERT INTO category_keywords (keyword, category) VALUES ('PASSLINE', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('TICKETPLUS', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('ENTRADA', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('ESTADIO', 'ENTERTAINMENT');

-- COMPRAS (SHOPPING)
INSERT INTO category_keywords (keyword, category) VALUES ('DECATHLON', 'SHOPPING');
INSERT INTO category_keywords (keyword, category) VALUES ('SPARTA', 'SHOPPING');
INSERT INTO category_keywords (keyword, category) VALUES ('DOITE', 'SHOPPING');

-- Seeding de metas de presupuesto (INE Chile) para el cálculo de desviaciones
INSERT INTO category_budget_targets (category, target_percentage, country_code, description) VALUES ('FOOD', 15.0, 'CL', 'Alimentación');
INSERT INTO category_budget_targets (category, target_percentage, country_code, description) VALUES ('TRANSPORT', 14.1, 'CL', 'Transporte');
INSERT INTO category_budget_targets (category, target_percentage, country_code, description) VALUES ('HOUSING', 14.3, 'CL', 'Vivienda');
INSERT INTO category_budget_targets (category, target_percentage, country_code, description) VALUES ('UTILITIES', 12.0, 'CL', 'Servicios básicos');
INSERT INTO category_budget_targets (category, target_percentage, country_code, description) VALUES ('ENTERTAINMENT', 7.0, 'CL', 'Entretenimiento y ocio');
INSERT INTO category_budget_targets (category, target_percentage, country_code, description) VALUES ('HEALTH', 5.0, 'CL', 'Salud');
INSERT INTO category_budget_targets (category, target_percentage, country_code, description) VALUES ('EDUCATION', 6.5, 'CL', 'Educación');
INSERT INTO category_budget_targets (category, target_percentage, country_code, description) VALUES ('SHOPPING', 12.1, 'CL', 'Compras y retail');
INSERT INTO category_budget_targets (category, target_percentage, country_code, description) VALUES ('OTHER_EXPENSE', 7.0, 'CL', 'Otros gastos');

