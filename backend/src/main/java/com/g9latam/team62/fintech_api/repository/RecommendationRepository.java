package com.g9latam.team62.fintech_api.repository;

import com.g9latam.team62.fintech_api.model.Recommendation;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class RecommendationRepository {

    private final Map<Long, Recommendation> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong();

    public Recommendation save(Recommendation recommendation) {
        if (recommendation.getId() == null) {
            recommendation.setId(idGenerator.incrementAndGet());
        }
        storage.put(recommendation.getId(), recommendation);
        return recommendation;
    }

    public Collection<Recommendation> findAll() {
        return storage.values();
    }

    public Optional<Recommendation> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    public List<Recommendation> findByUserId(Long userId) {
        return storage.values().stream()
                .filter(recommendation -> userId.equals(recommendation.getUserId()))
                .toList();
    }

    public void deleteById(Long id) {
        storage.remove(id);
    }

    public void deleteByUserId(Long userId) {
        storage.values().removeIf(recommendation -> userId.equals(recommendation.getUserId()));
    }
}
