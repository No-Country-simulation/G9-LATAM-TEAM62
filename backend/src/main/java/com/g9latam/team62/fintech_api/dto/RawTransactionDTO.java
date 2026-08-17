package com.g9latam.team62.fintech_api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RawTransactionDTO(
    @NotBlank(message = "La descripción de la transacción es obligatoria")
    String descripcion,

    @NotNull(message = "El valor/monto de la transacción es obligatorio")
    @DecimalMin(value = "0.01", message = "El valor de la transacción debe ser mayor a 0")
    BigDecimal valor
) {}
