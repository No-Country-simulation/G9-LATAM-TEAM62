package com.g9latam.team62.fintech_api.dto;

import com.g9latam.team62.fintech_api.model.FinancialProfile;
import com.g9latam.team62.fintech_api.model.SavingFrequency;
import com.g9latam.team62.fintech_api.model.User;

import java.math.BigDecimal;

public record UserResponseDTO(
        Long id,
        String name,
        String email,
        BigDecimal monthlyIncome,
        SavingFrequency savingFrequency,
        FinancialProfile financialProfile,
        Double profileAccuracy
) {
    public static UserResponseDTO fromEntity(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getMonthlyIncome(),
                user.getSavingFrequency(),
                user.getFinancialProfile(),
                user.getProfileAccuracy()
        );
    }
}