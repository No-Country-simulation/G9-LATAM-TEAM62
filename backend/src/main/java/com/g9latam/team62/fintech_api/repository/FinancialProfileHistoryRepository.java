package com.g9latam.team62.fintech_api.repository;

import com.g9latam.team62.fintech_api.model.FinancialProfileHistory;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class FinancialProfileHistoryRepository {

    private final Map<Long, FinancialProfileHistory> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong();

    public FinancialProfileHistory save(FinancialProfileHistory history) {
        if (history.getId() == null) {
            history.setId(idGenerator.incrementAndGet());
        }
        storage.put(history.getId(), history);
        return history;
    }

    public Collection<FinancialProfileHistory> findAll() {
        return storage.values();
    }

    public Optional<FinancialProfileHistory> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    public List<FinancialProfileHistory> findByUserIdOrderByCreatedAtDesc(Long userId) {
        return storage.values().stream()
                .filter(history -> userId.equals(history.getUserId()))
                .sorted(Comparator.comparing(FinancialProfileHistory::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }
}
