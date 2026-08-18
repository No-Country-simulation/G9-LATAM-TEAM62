package com.g9latam.team62.fintech_api.dto;

import com.g9latam.team62.fintech_api.model.Category;
import com.g9latam.team62.fintech_api.model.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record ManualTransactionRequest(
        @NotNull(message = "El ID de usuario es obligatorio") 
        Long userId,

        @NotNull(message = "El monto es obligatorio") 
        @Positive(message = "El monto debe ser un valor positivo mayor a cero") 
        BigDecimal amount,

        // Opcional: si se omite, el motor de 4 niveles la deduce de la descripción.
        Category category,

        @Size(min = 10, max = 200, message = "La descripción debe tener entre 10 y 200 caracteres")
        String description,

        // Opcional: por omisión se asume CLP, que es lo que el servicio ya aplicaba.
        CurrencyRef currency,

        @NotNull(message = "El método de pago es obligatorio")
        PaymentMethod paymentMethod,

        @Size(max = 100, message = "El nombre del banco no puede superar los 100 caracteres")
        String bankName,

        @Size(max = 50, message = "El número de operación no puede superar los 50 caracteres")
        @Pattern(regexp = "^[a-zA-Z0-9-]*$", message = "El número de operación solo puede contener letras, números y guiones")
        String operationNumber
) {}
