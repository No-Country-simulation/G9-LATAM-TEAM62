package com.g9latam.team62.fintech_api.dto;

import com.g9latam.team62.fintech_api.model.Category;
import jakarta.validation.constraints.NotNull;

// Payload de PUT /api/transactions/{id}/category. Cuando el usuario corrige
// la categoría sugerida (venga de un mapeo, una regla o el modelo), este es
// el único campo que puede cambiar por esta vía — no se toca amount, date,
// ni ningún otro dato de la transacción original.
public record CategoryCorrectionRequest(
        @NotNull Category category
) {
}
