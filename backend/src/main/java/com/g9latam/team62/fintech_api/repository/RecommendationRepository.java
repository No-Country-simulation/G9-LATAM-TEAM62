package com.g9latam.team62.fintech_api.repository;

import com.g9latam.team62.fintech_api.model.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    List<Recommendation> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}
