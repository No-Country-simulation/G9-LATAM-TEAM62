-- =============================================================================
-- ESQUEMA Y DATOS INICIALES PARA MYSQL / MARIADB (XAMPP / phpMyAdmin)
-- Base de Datos: fintech_db
-- =============================================================================

CREATE TABLE IF NOT EXISTS currencies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name_currency VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    monthly_income DECIMAL(19,2),
    saving_frequency VARCHAR(20),
    financial_profile VARCHAR(20),
    profile_accuracy DOUBLE,
    profile_updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    description VARCHAR(255),
    operation_number VARCHAR(100),
    amount DECIMAL(19,2) NOT NULL,
    category VARCHAR(50) NOT NULL,
    transaction_date DATE NOT NULL,
    currency_id BIGINT,
    balance_after DECIMAL(19,2),
    user_id BIGINT NOT NULL,
    source VARCHAR(10) DEFAULT 'BANK',
    payment_method VARCHAR(20),
    link_status VARCHAR(10) DEFAULT 'UNLINKED',
    linked_transaction_id BIGINT,
    category_method VARCHAR(20),
    category_confidence DOUBLE,
    bank_name VARCHAR(100),
    CONSTRAINT fk_transactions_currency
        FOREIGN KEY (currency_id) REFERENCES currencies (id),
    CONSTRAINT fk_transactions_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS recommendations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    text VARCHAR(1000) NOT NULL,
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    profile_at_generation VARCHAR(20),
    user_id BIGINT NOT NULL,
    CONSTRAINT fk_recommendations_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS transaction_category_mappings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    description_pattern VARCHAR(255) NOT NULL UNIQUE,
    category VARCHAR(50) NOT NULL,
    frequency INT DEFAULT 1
);

CREATE TABLE IF NOT EXISTS category_keywords (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    keyword VARCHAR(100) NOT NULL UNIQUE,
    category VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS category_budget_targets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(50) NOT NULL,
    target_percentage DECIMAL(5,2) NOT NULL,
    country_code VARCHAR(2) NOT NULL DEFAULT 'CL',
    description VARCHAR(255),
    CONSTRAINT uk_category_country UNIQUE (category, country_code)
);

CREATE TABLE IF NOT EXISTS financial_profile_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    financial_profile VARCHAR(20) NOT NULL,
    profile_accuracy DOUBLE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_profile_history_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- =============================================================================
-- DATOS INICIALES (SEED DATA)
-- =============================================================================

-- 1. Monedas principales
INSERT IGNORE INTO currencies (id, name_currency) VALUES
(1, 'USD'), (2, 'EUR'), (3, 'CLP'), (4, 'ARS'), (5, 'MXN'),
(6, 'COP'), (7, 'PEN'), (8, 'BRL'), (9, 'UYU'), (10, 'DOP'),
(11, 'CRC'), (12, 'GTQ'), (13, 'PYG'), (14, 'BOB'), (15, 'HNL'),
(16, 'NIO'), (17, 'VES'), (18, 'PAB');

-- 2. Usuarios de prueba (Contraseña para todos: password123)
INSERT IGNORE INTO users (id, name, email, password, monthly_income, saving_frequency, financial_profile, profile_accuracy, profile_updated_at) VALUES
(1, 'Juan Pérez', 'juan.perez@example.com', '$2a$10$UurvK/egFzuLq7ngVAB6u.4NDhH/r1PVvbCmZGhfpZPhyUL4vMozi', 2500000.00, 'MONTHLY', 'SAVER', 0.85, NOW()),
(2, 'María González', 'maria.gonzalez@example.com', '$2a$10$UurvK/egFzuLq7ngVAB6u.4NDhH/r1PVvbCmZGhfpZPhyUL4vMozi', 1800000.00, 'BIWEEKLY', 'BALANCED', 0.78, NOW()),
(3, 'Carlos Rodríguez', 'carlos.rodriguez@example.com', '$2a$10$UurvK/egFzuLq7ngVAB6u.4NDhH/r1PVvbCmZGhfpZPhyUL4vMozi', 950000.00, 'RARELY', 'SPENDER', 0.90, NOW()),
(4, 'Ana Martínez', 'ana.martinez@example.com', '$2a$10$UurvK/egFzuLq7ngVAB6u.4NDhH/r1PVvbCmZGhfpZPhyUL4vMozi', 600000.00, 'NEVER', 'AT_RISK', 0.95, NOW()),
(5, 'Lucas Silva', 'lucas.silva@example.com', '$2a$10$UurvK/egFzuLq7ngVAB6u.4NDhH/r1PVvbCmZGhfpZPhyUL4vMozi', 3200000.00, 'WEEKLY', 'SAVER', 0.88, NOW());

-- 3. Transacciones iniciales
INSERT IGNORE INTO transactions (id, description, operation_number, amount, category, transaction_date, currency_id, balance_after, user_id) VALUES
(1, 'SUELDO EMPRESA TECH', 'OP-1001', 2500000.00, 'SALARY', '2026-08-01', 3, 2500000.00, 1),
(2, 'GASTO COMUN EDIFICIO', 'OP-1002', 120000.00, 'HOUSING', '2026-08-02', 3, 2380000.00, 1),
(3, 'SUPERMERCADO JUMBO', 'OP-1003', 85000.50, 'FOOD', '2026-08-03', 3, 2294999.50, 1),
(4, 'PAGO LUZ ENEL', 'OP-1004', 35000.00, 'UTILITIES', '2026-08-04', 3, 2259999.50, 1),
(5, 'NETFLIX SUBSCRIPTION', 'OP-1005', 10990.00, 'ENTERTAINMENT', '2026-08-05', 3, 2249009.50, 1);

-- 4. Recomendaciones iniciales
INSERT IGNORE INTO recommendations (id, text, generated_at, profile_at_generation, user_id) VALUES
(1, 'Felicidades por mantener una tasa de ahorro constante. Te recomendamos diversificar en un fondo indexado.', NOW(), 'SAVER', 1),
(2, 'Tus gastos en entretenimiento representan un 15% de tus ingresos. Reducirlos un 5% aumentará tu fondo de emergencia.', NOW(), 'BALANCED', 2);
