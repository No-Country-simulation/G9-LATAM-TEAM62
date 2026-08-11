package com.g9latam.team62.fintech_api.service;

import com.g9latam.team62.fintech_api.model.Recommendation;
import com.g9latam.team62.fintech_api.model.User;
import com.g9latam.team62.fintech_api.repository.RecommendationRepository;
import com.g9latam.team62.fintech_api.repository.UserRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class RecommendationService {

    private final RecommendationRepository repository;
    private final UserRepository userRepository;

    public RecommendationService(RecommendationRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Recommendation create(Recommendation recommendation) {
        Long userId = java.util.Objects.requireNonNull(recommendation.getUserId(), "userId is required");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "user " + userId + " does not exist"));
        recommendation.setId(null);
        recommendation.setGeneratedAt(LocalDateTime.now());
        recommendation.setProfileAtGeneration(user.getFinancialProfile());
        return repository.save(recommendation);
    }

    public Collection<Recommendation> findAll() {
        return repository.findAll();
    }

    public Optional<Recommendation> findById(@NonNull Long id) {
        return repository.findById(id);
    }

    public List<Recommendation> findByUserId(@NonNull Long userId) {
        return repository.findByUserId(userId);
    }

    @Transactional
    public void delete(@NonNull Long id) {
        repository.deleteById(id);
    }
}
