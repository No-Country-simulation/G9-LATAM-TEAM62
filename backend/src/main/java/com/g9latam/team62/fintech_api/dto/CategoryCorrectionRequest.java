package com.g9latam.team62.fintech_api.dto;

import com.g9latam.team62.fintech_api.model.Category;
import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de la petición para el endpoint de corrección de categoría
 * {@code PUT /api/transactions/{id}/category}.
 *
 * @param category la nueva categoría corregida seleccionada por el usuario
 */
public record CategoryCorrectionRequest(
        @NotNull Category category) {
}
