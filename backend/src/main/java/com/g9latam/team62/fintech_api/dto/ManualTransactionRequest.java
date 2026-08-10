package com.g9latam.team62.fintech_api.dto;

import com.g9latam.team62.fintech_api.model.Category;
import com.g9latam.team62.fintech_api.model.Currency;
import com.g9latam.team62.fintech_api.model.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

// Payload del registro manual (alternativa 1): el usuario aporta monto,
// categoría, medio de pago y moneda; la fecha la asigna el servidor
// (siempre "hoy"), y el número de operación no existe porque no viene de
// ningún banco. `currency` sigue el mismo contrato que ya usa
// TransactionService.resolveCurrency(): {"id": 1} o {"name_currency": "CLP"}.
public record ManualTransactionRequest(
        @NotNull Long userId,
        @NotNull @Positive BigDecimal amount,
        @NotNull Category category,
        @NotNull PaymentMethod paymentMethod,
        @NotNull Currency currency,
        String description // opcional; si el usuario lo completa, sirve a futuro para reentrenar el clasificador
) {
}
