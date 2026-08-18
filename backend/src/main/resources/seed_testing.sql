-- Seed data for the dev (H2) profile only; spring.sql.init.mode=never on Oracle,
-- where db/oracle/data.sql does the same job.

-- 1. MONEDAS (CURRENCIES)
INSERT INTO currencies (name_currency) VALUES ('CLP');
INSERT INTO currencies (name_currency) VALUES ('USD');
INSERT INTO currencies (name_currency) VALUES ('EUR');

-- 5. METAS PRESUPUESTARIAS (CATEGORY_BUDGET_TARGETS)
INSERT INTO category_budget_targets (category, target_percentage, country_code, description) VALUES ('FOOD', 15.0, 'CL', 'Alimentación');
INSERT INTO category_budget_targets (category, target_percentage, country_code, description) VALUES ('TRANSPORT', 14.1, 'CL', 'Transporte');
INSERT INTO category_budget_targets (category, target_percentage, country_code, description) VALUES ('HOUSING', 14.3, 'CL', 'Vivienda');
INSERT INTO category_budget_targets (category, target_percentage, country_code, description) VALUES ('UTILITIES', 12.0, 'CL', 'Servicios básicos');
INSERT INTO category_budget_targets (category, target_percentage, country_code, description) VALUES ('ENTERTAINMENT', 7.0, 'CL', 'Entretenimiento y ocio');
INSERT INTO category_budget_targets (category, target_percentage, country_code, description) VALUES ('HEALTH', 5.0, 'CL', 'Salud');
INSERT INTO category_budget_targets (category, target_percentage, country_code, description) VALUES ('EDUCATION', 6.5, 'CL', 'Educación');
INSERT INTO category_budget_targets (category, target_percentage, country_code, description) VALUES ('SHOPPING', 12.1, 'CL', 'Compras y retail');
INSERT INTO category_budget_targets (category, target_percentage, country_code, description) VALUES ('OTHER_EXPENSE', 7.0, 'CL', 'Otros gastos');

-- =============================================================================
-- 6. MAPEOS DE CATEGORÍAS (CATEGORY_MAPPINGS)
-- =============================================================================
-- Mapeos para ENTERTAINMENT
INSERT INTO category_mappings (description_pattern, category, frequency) VALUES ('APP STORE', 'ENTERTAINMENT', 90);
INSERT INTO category_mappings (description_pattern, category, frequency) VALUES ('APPLE BILL', 'ENTERTAINMENT', 90);
INSERT INTO category_mappings (description_pattern, category, frequency) VALUES ('APPSTORE', 'ENTERTAINMENT', 90);
INSERT INTO category_mappings (description_pattern, category, frequency) VALUES ('BETANO', 'ENTERTAINMENT', 60);
INSERT INTO category_mappings (description_pattern, category, frequency) VALUES ('COOLBET', 'ENTERTAINMENT', 60);
INSERT INTO category_mappings (description_pattern, category, frequency) VALUES ('CRUNCHYROLL', 'ENTERTAINMENT', 70);
INSERT INTO category_mappings (description_pattern, category, frequency) VALUES ('GOOGLE PLAY', 'ENTERTAINMENT', 90);
INSERT INTO category_mappings (description_pattern, category, frequency) VALUES ('HAPPYLAND', 'ENTERTAINMENT', 55);
INSERT INTO category_mappings (description_pattern, category, frequency) VALUES ('HBO MAX', 'ENTERTAINMENT', 90);
INSERT INTO category_mappings (description_pattern, category, frequency) VALUES ('MAX STREAMING', 'ENTERTAINMENT', 80);
INSERT INTO category_mappings (description_pattern, category, frequency) VALUES ('PLAY STORE', 'ENTERTAINMENT', 90);
INSERT INTO category_mappings (description_pattern, category, frequency) VALUES ('PLAYSTORE', 'ENTERTAINMENT', 90);
INSERT INTO category_mappings (description_pattern, category, frequency) VALUES ('STEAM', 'ENTERTAINMENT', 95);
INSERT INTO category_mappings (description_pattern, category, frequency) VALUES ('TWITCH', 'ENTERTAINMENT', 65);

-- Mapeos para FOOD
INSERT INTO category_mappings (description_pattern, category, frequency) VALUES ('ACUENTA', 'FOOD', 75);
INSERT INTO category_mappings (description_pattern, category, frequency) VALUES ('ALVI', 'FOOD', 60);
INSERT INTO category_mappings (description_pattern, category, frequency) VALUES ('CASTANO', 'FOOD', 70);
INSERT INTO category_mappings (description_pattern, category, frequency) VALUES ('DOGGIS', 'FOOD', 60);
INSERT INTO category_mappings (description_pattern, category, frequency) VALUES ('JUAN MAESTRO', 'FOOD', 50);
INSERT INTO category_mappings (description_pattern, category, frequency) VALUES ('OK MARKET', 'FOOD', 70);
INSERT INTO category_mappings (description_pattern, category, frequency) VALUES ('OXXO', 'FOOD', 80);
INSERT INTO category_mappings (description_pattern, category, frequency) VALUES ('PEDRO JUAN Y DIEGO', 'FOOD', 45);
INSERT INTO category_mappings (description_pattern, category, frequency) VALUES ('TARRAGONA', 'FOOD', 40);

-- Mapeos para INVESTMENT
INSERT INTO category_mappings (description_pattern, category, frequency) VALUES ('FINTUAL', 'INVESTMENT', 80);

-- Mapeos para SHOPPING
INSERT INTO category_mappings (description_pattern, category, frequency) VALUES ('CHILEMAT', 'SHOPPING', 45);
INSERT INTO category_mappings (description_pattern, category, frequency) VALUES ('CONSTRUMART', 'SHOPPING', 50);
INSERT INTO category_mappings (description_pattern, category, frequency) VALUES ('CORONA', 'SHOPPING', 50);
INSERT INTO category_mappings (description_pattern, category, frequency) VALUES ('EASY', 'SHOPPING', 70);
INSERT INTO category_mappings (description_pattern, category, frequency) VALUES ('HITES', 'SHOPPING', 60);
INSERT INTO category_mappings (description_pattern, category, frequency) VALUES ('IMPERIAL', 'SHOPPING', 55);
INSERT INTO category_mappings (description_pattern, category, frequency) VALUES ('LA POLAR', 'SHOPPING', 55);
INSERT INTO category_mappings (description_pattern, category, frequency) VALUES ('MTS', 'SHOPPING', 40);
INSERT INTO category_mappings (description_pattern, category, frequency) VALUES ('PC FACTORY', 'SHOPPING', 65);
INSERT INTO category_mappings (description_pattern, category, frequency) VALUES ('SODIMAC', 'SHOPPING', 80);
INSERT INTO category_mappings (description_pattern, category, frequency) VALUES ('TRICOT', 'SHOPPING', 50);

-- =============================================================================
-- 7. PALABRAS CLAVE (CATEGORY_KEYWORDS)
-- =============================================================================
-- Palabras Clave para EDUCATION
INSERT INTO category_keywords (keyword, category) VALUES ('COLEGIO', 'EDUCATION');
INSERT INTO category_keywords (keyword, category) VALUES ('DUOLINGO', 'EDUCATION');
INSERT INTO category_keywords (keyword, category) VALUES ('OPEN ENGLISH', 'EDUCATION');
INSERT INTO category_keywords (keyword, category) VALUES ('UNIVERSIDAD', 'EDUCATION');

-- Palabras Clave para ENTERTAINMENT
INSERT INTO category_keywords (keyword, category) VALUES ('APP STORE', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('APPLE BILL', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('APPLE MUSIC', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('APPSTORE', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('BATTLE NET', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('BATTLENET', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('BETANO', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('BLIZZARD', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('CINE', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('COOLBET', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('CRUNCHYROLL', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('DEEZER', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('DISCORD', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('DISCOTECA', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('DISCOTEQUE', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('ENTRADA', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('EPIC GAMES', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('EPICGAMES', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('ESTADIO', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('GOG COM', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('GOOGLE PLAY', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('GOOGLEPLAY', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('HAPPYLAND', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('HBO MAX', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('ITUNES', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('JUEGOS DIANA', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('MAX STREAMING', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('NETFLIX', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('NINTENDO', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('ORIGIN', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('PASSLINE', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('PLAY STORE', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('PLAYSTATION', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('PLAYSTORE', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('PRIME VIDEO', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('PRIMEVIDEO', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('PS PLUS', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('PSN', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('PUNTOTICKET', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('RIOT GAMES', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('ROBLOX', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('SOUNDCLOUD', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('SPOTIFY', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('STEAM', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('STREAMING', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('TICKETPLUS', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('TIDAL', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('TWITCH', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('UBISOFT', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('UPLAY', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('VALORANT', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('XBOX', 'ENTERTAINMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('YOUTUBE', 'ENTERTAINMENT');

-- Palabras Clave para FOOD
INSERT INTO category_keywords (keyword, category) VALUES ('ACUENTA', 'FOOD');
INSERT INTO category_keywords (keyword, category) VALUES ('ALIMENTACION', 'FOOD');
INSERT INTO category_keywords (keyword, category) VALUES ('ALVI', 'FOOD');
INSERT INTO category_keywords (keyword, category) VALUES ('CASINO', 'FOOD');
INSERT INTO category_keywords (keyword, category) VALUES ('CASTANO', 'FOOD');
INSERT INTO category_keywords (keyword, category) VALUES ('DELIVERY', 'FOOD');
INSERT INTO category_keywords (keyword, category) VALUES ('DOGGIS', 'FOOD');
INSERT INTO category_keywords (keyword, category) VALUES ('ERBI', 'FOOD');
INSERT INTO category_keywords (keyword, category) VALUES ('FUENTEMAYOR', 'FOOD');
INSERT INTO category_keywords (keyword, category) VALUES ('JUAN MAESTRO', 'FOOD');
INSERT INTO category_keywords (keyword, category) VALUES ('JUMBO', 'FOOD');
INSERT INTO category_keywords (keyword, category) VALUES ('LIDER', 'FOOD');
INSERT INTO category_keywords (keyword, category) VALUES ('LO SALDES', 'FOOD');
INSERT INTO category_keywords (keyword, category) VALUES ('OK MARKET', 'FOOD');
INSERT INTO category_keywords (keyword, category) VALUES ('OXXO', 'FOOD');
INSERT INTO category_keywords (keyword, category) VALUES ('PEDRO JUAN Y DIEGO', 'FOOD');
INSERT INTO category_keywords (keyword, category) VALUES ('PIZZA HUT', 'FOOD');
INSERT INTO category_keywords (keyword, category) VALUES ('RESTAURANT', 'FOOD');
INSERT INTO category_keywords (keyword, category) VALUES ('SAN CAMILO', 'FOOD');
INSERT INTO category_keywords (keyword, category) VALUES ('SANGUCHERIA', 'FOOD');
INSERT INTO category_keywords (keyword, category) VALUES ('SCHOPDOG', 'FOOD');
INSERT INTO category_keywords (keyword, category) VALUES ('SUPERMERCADO', 'FOOD');
INSERT INTO category_keywords (keyword, category) VALUES ('SUSHI', 'FOOD');
INSERT INTO category_keywords (keyword, category) VALUES ('TARRAGONA', 'FOOD');
INSERT INTO category_keywords (keyword, category) VALUES ('TOTTUS', 'FOOD');
INSERT INTO category_keywords (keyword, category) VALUES ('UNIMARC', 'FOOD');
INSERT INTO category_keywords (keyword, category) VALUES ('WENDYS', 'FOOD');

-- Palabras Clave para HEALTH
INSERT INTO category_keywords (keyword, category) VALUES ('ACHS', 'HEALTH');
INSERT INTO category_keywords (keyword, category) VALUES ('CLINICA', 'HEALTH');
INSERT INTO category_keywords (keyword, category) VALUES ('CLINICA ALEMANA', 'HEALTH');
INSERT INTO category_keywords (keyword, category) VALUES ('CLINICA DAVILA', 'HEALTH');
INSERT INTO category_keywords (keyword, category) VALUES ('CLINICA LAS CONDES', 'HEALTH');
INSERT INTO category_keywords (keyword, category) VALUES ('CLINICA SANTA MARIA', 'HEALTH');
INSERT INTO category_keywords (keyword, category) VALUES ('CONSALUD', 'HEALTH');
INSERT INTO category_keywords (keyword, category) VALUES ('DOCTOR SIMI', 'HEALTH');
INSERT INTO category_keywords (keyword, category) VALUES ('FARMACIA', 'HEALTH');
INSERT INTO category_keywords (keyword, category) VALUES ('FARMACIA POPULAR', 'HEALTH');
INSERT INTO category_keywords (keyword, category) VALUES ('FARMACIA SIMI', 'HEALTH');
INSERT INTO category_keywords (keyword, category) VALUES ('FARMACIAS KNOP', 'HEALTH');
INSERT INTO category_keywords (keyword, category) VALUES ('MEDICO', 'HEALTH');
INSERT INTO category_keywords (keyword, category) VALUES ('MEDS', 'HEALTH');
INSERT INTO category_keywords (keyword, category) VALUES ('MUTUAL', 'HEALTH');
INSERT INTO category_keywords (keyword, category) VALUES ('VIDA TRES', 'HEALTH');

-- Palabras Clave para HOUSING
INSERT INTO category_keywords (keyword, category) VALUES ('ARRIENDO', 'HOUSING');
INSERT INTO category_keywords (keyword, category) VALUES ('CASA', 'HOUSING');
INSERT INTO category_keywords (keyword, category) VALUES ('CONDOMINIO', 'HOUSING');
INSERT INTO category_keywords (keyword, category) VALUES ('CORREDOR', 'HOUSING');
INSERT INTO category_keywords (keyword, category) VALUES ('DEPARTAMENTO', 'HOUSING');
INSERT INTO category_keywords (keyword, category) VALUES ('DIVIDENDO', 'HOUSING');
INSERT INTO category_keywords (keyword, category) VALUES ('EDIFICIO', 'HOUSING');
INSERT INTO category_keywords (keyword, category) VALUES ('MUDANZA', 'HOUSING');
INSERT INTO category_keywords (keyword, category) VALUES ('PAGO DEPARTAMENTO', 'HOUSING');
INSERT INTO category_keywords (keyword, category) VALUES ('VIVIENDA', 'HOUSING');

-- Palabras Clave para INVESTMENT
INSERT INTO category_keywords (keyword, category) VALUES ('DEPOSITO A PLAZO', 'INVESTMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('FINTUAL', 'INVESTMENT');
INSERT INTO category_keywords (keyword, category) VALUES ('FONDO MUTUO', 'INVESTMENT');

-- Palabras Clave para OTHER_INCOME
INSERT INTO category_keywords (keyword, category) VALUES ('DEVOLUCION', 'OTHER_INCOME');
INSERT INTO category_keywords (keyword, category) VALUES ('REEMBOLSO', 'OTHER_INCOME');
INSERT INTO category_keywords (keyword, category) VALUES ('TGR', 'OTHER_INCOME');
INSERT INTO category_keywords (keyword, category) VALUES ('TRANSFERENCIA', 'OTHER_INCOME');

-- Palabras Clave para SALARY
INSERT INTO category_keywords (keyword, category) VALUES ('REMUNERACION', 'SALARY');
INSERT INTO category_keywords (keyword, category) VALUES ('SUELDO', 'SALARY');

-- Palabras Clave para SAVINGS
INSERT INTO category_keywords (keyword, category) VALUES ('AHORRO', 'SAVINGS');
INSERT INTO category_keywords (keyword, category) VALUES ('APV', 'SAVINGS');
INSERT INTO category_keywords (keyword, category) VALUES ('CUENTA DE AHORRO', 'SAVINGS');

-- Palabras Clave para SHOPPING
INSERT INTO category_keywords (keyword, category) VALUES ('ALTO LAS CONDES', 'SHOPPING');
INSERT INTO category_keywords (keyword, category) VALUES ('AUTOPLANET', 'SHOPPING');
INSERT INTO category_keywords (keyword, category) VALUES ('CHILEMAT', 'SHOPPING');
INSERT INTO category_keywords (keyword, category) VALUES ('COMPRA', 'SHOPPING');
INSERT INTO category_keywords (keyword, category) VALUES ('CONSTRUMART', 'SHOPPING');
INSERT INTO category_keywords (keyword, category) VALUES ('CORONA', 'SHOPPING');
INSERT INTO category_keywords (keyword, category) VALUES ('COSTANERA CENTER', 'SHOPPING');
INSERT INTO category_keywords (keyword, category) VALUES ('DECATHLON', 'SHOPPING');
INSERT INTO category_keywords (keyword, category) VALUES ('DOITE', 'SHOPPING');
INSERT INTO category_keywords (keyword, category) VALUES ('FERRETERIA', 'SHOPPING');
INSERT INTO category_keywords (keyword, category) VALUES ('HITES', 'SHOPPING');
INSERT INTO category_keywords (keyword, category) VALUES ('IMPERIAL', 'SHOPPING');
INSERT INTO category_keywords (keyword, category) VALUES ('LA POLAR', 'SHOPPING');
INSERT INTO category_keywords (keyword, category) VALUES ('MALL', 'SHOPPING');
INSERT INTO category_keywords (keyword, category) VALUES ('MALL CHINO', 'SHOPPING');
INSERT INTO category_keywords (keyword, category) VALUES ('MALLPLAZA', 'SHOPPING');
INSERT INTO category_keywords (keyword, category) VALUES ('MTS', 'SHOPPING');
INSERT INTO category_keywords (keyword, category) VALUES ('PARQUE ARAUCO', 'SHOPPING');
INSERT INTO category_keywords (keyword, category) VALUES ('PC FACTORY', 'SHOPPING');
INSERT INTO category_keywords (keyword, category) VALUES ('PCFACTORY', 'SHOPPING');
INSERT INTO category_keywords (keyword, category) VALUES ('SPARTA', 'SHOPPING');
INSERT INTO category_keywords (keyword, category) VALUES ('TRICOT', 'SHOPPING');

-- Palabras Clave para TRANSPORT
INSERT INTO category_keywords (keyword, category) VALUES ('AUTOPISTA CENTRAL', 'TRANSPORT');
INSERT INTO category_keywords (keyword, category) VALUES ('AUTOPISTA DEL SOL', 'TRANSPORT');
INSERT INTO category_keywords (keyword, category) VALUES ('BENCINA', 'TRANSPORT');
INSERT INTO category_keywords (keyword, category) VALUES ('BIP', 'TRANSPORT');
INSERT INTO category_keywords (keyword, category) VALUES ('CABIFY', 'TRANSPORT');
INSERT INTO category_keywords (keyword, category) VALUES ('COLECTIVO', 'TRANSPORT');
INSERT INTO category_keywords (keyword, category) VALUES ('COPEC', 'TRANSPORT');
INSERT INTO category_keywords (keyword, category) VALUES ('DIDI', 'TRANSPORT');
INSERT INTO category_keywords (keyword, category) VALUES ('EFE', 'TRANSPORT');
INSERT INTO category_keywords (keyword, category) VALUES ('METRO', 'TRANSPORT');
INSERT INTO category_keywords (keyword, category) VALUES ('PAGO TAG', 'TRANSPORT');
INSERT INTO category_keywords (keyword, category) VALUES ('PRT', 'TRANSPORT');
INSERT INTO category_keywords (keyword, category) VALUES ('RUTAS DEL MAIPO', 'TRANSPORT');
INSERT INTO category_keywords (keyword, category) VALUES ('TAG', 'TRANSPORT');
INSERT INTO category_keywords (keyword, category) VALUES ('TRANSANTIAGO', 'TRANSPORT');
INSERT INTO category_keywords (keyword, category) VALUES ('TREN', 'TRANSPORT');
INSERT INTO category_keywords (keyword, category) VALUES ('UBER', 'TRANSPORT');

-- Palabras Clave para UTILITIES
INSERT INTO category_keywords (keyword, category) VALUES ('AGUA', 'UTILITIES');
INSERT INTO category_keywords (keyword, category) VALUES ('DIRECTV', 'UTILITIES');
INSERT INTO category_keywords (keyword, category) VALUES ('GAS', 'UTILITIES');
INSERT INTO category_keywords (keyword, category) VALUES ('GTD MANQUEHUE', 'UTILITIES');
INSERT INTO category_keywords (keyword, category) VALUES ('INTERNET', 'UTILITIES');
INSERT INTO category_keywords (keyword, category) VALUES ('LUZ', 'UTILITIES');
INSERT INTO category_keywords (keyword, category) VALUES ('MOVIL', 'UTILITIES');
INSERT INTO category_keywords (keyword, category) VALUES ('MOVISTAR HOGAR', 'UTILITIES');
INSERT INTO category_keywords (keyword, category) VALUES ('MUNDO PACIFICO', 'UTILITIES');
INSERT INTO category_keywords (keyword, category) VALUES ('PAGO FACIL', 'UTILITIES');
INSERT INTO category_keywords (keyword, category) VALUES ('SENCILLITO', 'UTILITIES');
INSERT INTO category_keywords (keyword, category) VALUES ('SERVIPAG', 'UTILITIES');
INSERT INTO category_keywords (keyword, category) VALUES ('TELEFONICA', 'UTILITIES');
INSERT INTO category_keywords (keyword, category) VALUES ('TELSUR', 'UTILITIES');
INSERT INTO category_keywords (keyword, category) VALUES ('UNIRED', 'UTILITIES');

