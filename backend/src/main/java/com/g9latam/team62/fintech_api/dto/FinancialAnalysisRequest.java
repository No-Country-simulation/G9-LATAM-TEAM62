package com.g9latam.team62.fintech_api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record FinancialAnalysisRequest(
    @NotNull(message = "El ingreso mensual es obligatorio")
    @DecimalMin(value = "0.0", message = "El ingreso mensual debe ser mayor o igual a 0")
    @JsonProperty("ingreso_mensual") BigDecimal ingresoMensual,

    @NotNull(message = "El nivel de endeudamiento es obligatorio")
    @DecimalMin(value = "0.0", message = "El nivel de endeudamiento debe ser mayor o igual a 0")
    @JsonProperty("nivel_endeudamiento") BigDecimal nivelEndeudamiento,

    @NotBlank(message = "La frecuencia de ahorro es obligatoria")
    @JsonProperty("frecuencia_ahorro") String frecuenciaAhorro,

    @NotEmpty(message = "La lista de transacciones no puede estar vacía")
    @Valid
    List<RawTransactionDTO> transacciones
) {}
