package com.g9latam.team62.fintech_api.service;

import com.g9latam.team62.fintech_api.model.Category;
import com.g9latam.team62.fintech_api.model.CategoryMethod;

/**
 * Resultado de clasificar una descripción de transacción a través de la
 * jerarquía de 4 niveles.
 *
 * <p>{@code method} es el enum y no una cadena a propósito: cuando era texto libre, el
 * clasificador devolvía "BD_MAPPING", un valor que {@link CategoryMethod} nunca tuvo, y
 * la conversión fallaba en silencio guardando {@code FALLBACK} sobre aciertos de nivel 1.
 * Con el tipo real, esa discrepancia no compila.
 *
 * @param category   la categoría asignada
 * @param confidence nivel de confianza (1.0 para coincidencia BD, 0.9 para
 *                   reglas de palabras clave, probabilidad para ML, 0.0 para fallback)
 * @param method     nivel que resolvió la clasificación
 */
public record ClassificationResult(
        Category category,
        double confidence,
        CategoryMethod method) {
}
