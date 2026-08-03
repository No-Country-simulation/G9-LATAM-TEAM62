package com.g9latam.team62.fintech_api.dto;

import com.g9latam.team62.fintech_api.model.Category;

/**
 * Resultado de clasificar una descripción de transacción a través de la
 * jerarquía de 4 niveles.
 *
 * @param category   la categoría asignada
 * @param confidence nivel de confianza (1.0 para coincidencia BD, 0.9 para
 *                   reglas de palabras clave, probabilidad para ML, 0.0 para fallback)
 * @param method     método o nivel que resolvió la clasificación (etiqueta informativa)
 */
public record ClassificationResult(
        Category category,
        double confidence,
        String method) {
}
