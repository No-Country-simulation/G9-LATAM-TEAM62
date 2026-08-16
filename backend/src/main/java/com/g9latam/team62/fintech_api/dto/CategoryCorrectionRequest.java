package com.g9latam.team62.fintech_api.dto;

import com.g9latam.team62.fintech_api.model.Category;
import jakarta.validation.constraints.NotNull;

public record CategoryCorrectionRequest(
        @NotNull(message = "La categoría es obligatoria") Category category
) {}
