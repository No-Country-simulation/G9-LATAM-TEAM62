package com.g9latam.team62.fintech_api.dto;

import com.g9latam.team62.fintech_api.model.FinancialProfile;
import com.g9latam.team62.fintech_api.model.Recommendation;

import java.time.LocalDateTime;
import java.util.List;

/** Recomendación tal como la ve el cliente. */
public record RecommendationResponse(
        Long id,
        String text,
        LocalDateTime generatedAt,
        FinancialProfile profileAtGeneration,
        Long userId
) {

    public static RecommendationResponse fromEntity(Recommendation recommendation) {
        if (recommendation == null) {
            return null;
        }
        return new RecommendationResponse(
                recommendation.getId(),
                recommendation.getText(),
                recommendation.getGeneratedAt(),
                recommendation.getProfileAtGeneration(),
                recommendation.getUserId());
    }

    public static List<RecommendationResponse> fromEntities(List<Recommendation> recommendations) {
        return recommendations.stream().map(RecommendationResponse::fromEntity).toList();
    }
}
