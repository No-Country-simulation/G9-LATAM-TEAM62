package com.g9latam.team62.fintech_api.dto;

import com.g9latam.team62.fintech_api.model.FinancialProfile;
import com.g9latam.team62.fintech_api.model.FinancialProfileHistory;

import java.time.LocalDateTime;
import java.util.List;

/** Una entrada del historial de perfiles financieros del usuario. */
public record ProfileHistoryResponse(
        Long id,
        Long userId,
        FinancialProfile financialProfile,
        Double profileAccuracy,
        LocalDateTime createdAt
) {

    public static ProfileHistoryResponse fromEntity(FinancialProfileHistory entry) {
        if (entry == null) {
            return null;
        }
        return new ProfileHistoryResponse(
                entry.getId(),
                entry.getUserId(),
                entry.getFinancialProfile(),
                entry.getProfileAccuracy(),
                entry.getCreatedAt());
    }

    public static List<ProfileHistoryResponse> fromEntities(List<FinancialProfileHistory> entries) {
        return entries.stream().map(ProfileHistoryResponse::fromEntity).toList();
    }
}
