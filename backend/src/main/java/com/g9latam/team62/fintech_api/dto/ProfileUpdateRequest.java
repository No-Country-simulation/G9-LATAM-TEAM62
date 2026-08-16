package com.g9latam.team62.fintech_api.dto;

import java.math.BigDecimal;

import com.g9latam.team62.fintech_api.model.FinancialProfile;
import com.g9latam.team62.fintech_api.model.SavingFrequency;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record ProfileUpdateRequest(
        @NotNull(message = "El perfil financiero es obligatorio")
        @Valid 
        FinancialProfile financialProfile,

        @NotNull(message = "El nivel de precisión del perfil es obligatorio") 
        @DecimalMin(value = "0.0", message = "La precisión no puede ser menor a 0.0") 
        @DecimalMax(value = "1.0", message = "La precisión no puede superar 1.0") 
        BigDecimal profileAccuracy,

        @NotNull(message = "La frecuencia de ahorro es obligatoria")
        SavingFrequency savingFrequency) {
}
