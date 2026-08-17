package com.g9latam.team62.fintech_api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record FinancialAnalysisResponse(
    @JsonProperty("perfil_financiero") String perfilFinanciero,
    BigDecimal probabilidad,
    @JsonProperty("resumen_gastos") Map<String, BigDecimal> resumenGastos,
    List<String> recomendaciones
) {}
