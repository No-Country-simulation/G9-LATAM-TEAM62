package com.g9latam.team62.fintech_api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.g9latam.team62.fintech_api.model.Category;
import com.g9latam.team62.fintech_api.model.FinancialProfile;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Respuesta del análisis financiero en el formato que espera el caso de estudio.
 *
 * <p>La traducción del dominio a este vocabulario en español vive en {@link #of}: es parte del
 * contrato de la API, no de la lógica de perfilamiento, y tenerla aquí permite probarla sin
 * levantar MockMvc.
 *
 * @param probabilidad confianza del perfil entre 0 y 1. Es {@code double} y no {@code BigDecimal}
 *                     porque es un puntaje, no dinero: el mismo valor viaja como {@code double}
 *                     en {@code ClassificationResult} y se guarda en una columna FLOAT.
 */
public record FinancialAnalysisResponse(
    @JsonProperty("perfil_financiero") String perfilFinanciero,
    double probabilidad,
    @JsonProperty("resumen_gastos") Map<String, BigDecimal> resumenGastos,
    List<String> recomendaciones
) {

    public static FinancialAnalysisResponse of(FinancialProfile profile,
                                               double probabilidad,
                                               Map<Category, BigDecimal> gastosPorCategoria,
                                               List<String> recomendaciones) {
        Map<String, BigDecimal> resumen = new LinkedHashMap<>();
        gastosPorCategoria.forEach((category, total) ->
                resumen.merge(traducirCategoria(category), total, BigDecimal::add));

        return new FinancialAnalysisResponse(traducirPerfil(profile), probabilidad, resumen, recomendaciones);
    }

    private static String traducirPerfil(FinancialProfile profile) {
        return switch (profile) {
            case SAVER -> "Saludable";
            case BALANCED, SPENDER -> "En observación";
            case AT_RISK -> "En riesgo";
        };
    }

    private static String traducirCategoria(Category category) {
        return switch (category) {
            case FOOD -> "alimentacion";
            case TRANSPORT -> "transporte";
            case HOUSING -> "vivienda";
            case UTILITIES -> "servicios";
            case ENTERTAINMENT -> "entretenimiento";
            case HEALTH -> "salud";
            case EDUCATION -> "educacion";
            case SHOPPING -> "compras";
            default -> "otros_gastos";
        };
    }
}
