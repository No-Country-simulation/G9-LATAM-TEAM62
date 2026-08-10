package com.g9latam.team62.fintech_api.model;

// Medio de pago de una transacción. Tarjeta de crédito queda fuera del MVP a
// propósito: el estado de cuenta de crédito tiene un ciclo de facturación
// distinto al de la cuenta corriente, y el job de conciliación (matching)
// contra la cartola bancaria todavía no lo soporta.
public enum PaymentMethod {
    CASH,
    DEBIT
}
