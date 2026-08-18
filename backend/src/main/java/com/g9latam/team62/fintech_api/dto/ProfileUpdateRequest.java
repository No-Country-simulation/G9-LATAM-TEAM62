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
        Double profileAccuracy,

        @NotNull(message = "La frecuencia de ahorro es obligatoria")
        SavingFrequency savingFrequency,

        /**
         * Ingreso mensual con el que se calculó el perfil. Opcional: {@code null} deja el valor
         * que ya tuviera el usuario, de modo que {@code PUT /api/users/{id}/profile} sigue
         * funcionando sin enviarlo.
         */
        @DecimalMin(value = "0.0", message = "El ingreso mensual no puede ser negativo")
        BigDecimal monthlyIncome) {

    /** Perfil sin tocar el ingreso previamente registrado. */
    public ProfileUpdateRequest(FinancialProfile financialProfile,
                                Double profileAccuracy,
                                SavingFrequency savingFrequency) {
        this(financialProfile, profileAccuracy, savingFrequency, null);
    }
}
