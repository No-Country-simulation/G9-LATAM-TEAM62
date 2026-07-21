package com.g9latam.team62.fintech_api.dto;

import com.g9latam.team62.fintech_api.model.FinancialProfile;
import com.g9latam.team62.fintech_api.model.SavingFrequency;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

// payload sent by the external profiling app; savingFrequency is optional and
// only applied when the external app computed it
public record ProfileUpdateRequest(
        @NotNull FinancialProfile financialProfile,
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double profileAccuracy,
        SavingFrequency savingFrequency) {
}
