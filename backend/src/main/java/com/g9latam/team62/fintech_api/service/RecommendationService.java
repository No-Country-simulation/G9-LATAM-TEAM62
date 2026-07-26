package com.g9latam.team62.fintech_api.service;

import com.g9latam.team62.fintech_api.model.Recommendation;
import com.g9latam.team62.fintech_api.model.User;
import com.g9latam.team62.fintech_api.repository.RecommendationRepository;
import com.g9latam.team62.fintech_api.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
public class RecommendationService {

    private final RecommendationRepository repository;
    private final UserRepository userRepository;

    public RecommendationService(RecommendationRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    public Recommendation create(Recommendation recommendation) {
        User user = userRepository.findById(recommendation.getUserId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "user " + recommendation.getUserId() + " does not exist"));
        recommendation.setId(null);
        recommendation.setGeneratedAt(LocalDateTime.now());
        recommendation.setProfileAtGeneration(user.getFinancialProfile());
        return repository.save(recommendation);
    }

    public Collection<Recommendation> findAll() {
        return repository.findAll();
    }

    public Optional<Recommendation> findById(Long id) {
        return repository.findById(id);
    }

    public List<Recommendation> findByUserId(Long userId) {
        return repository.findByUserId(userId);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
