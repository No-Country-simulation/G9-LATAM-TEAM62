package com.g9latam.team62.fintech_api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.util.List;

public record FinancialAnalysisRequest(
    @DecimalMin(value = "0.0", message = "El ingreso mensual debe ser mayor o igual a 0")
    @JsonProperty("ingreso_mensual") BigDecimal ingresoMensual,

    @DecimalMin(value = "0.0", message = "El nivel de endeudamiento debe ser mayor o igual a 0")
    @JsonProperty("nivel_endeudamiento") BigDecimal nivelEndeudamiento,

    @JsonProperty("frecuencia_ahorro") String frecuenciaAhorro,

    @JsonProperty("transaction_ids") List<Long> transactionIds
) {
    public FinancialAnalysisRequest(BigDecimal ingresoMensual, BigDecimal nivelEndeudamiento, String frecuenciaAhorro) {
        this(ingresoMensual, nivelEndeudamiento, frecuenciaAhorro, null);
    }
}
