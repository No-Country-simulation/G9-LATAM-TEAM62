package com.g9latam.team62.fintech_api.repository;

import com.g9latam.team62.fintech_api.model.Transaction;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class TransactionRepository {

    private final Map<Long, Transaction> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong();

    public Transaction save(Transaction transaction) {
        if (transaction.getId() == null) {
            transaction.setId(idGenerator.incrementAndGet());
        }
        storage.put(transaction.getId(), transaction);
        return transaction;
    }

    public Collection<Transaction> findAll() {
        return storage.values();
    }

    public Optional<Transaction> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    public List<Transaction> findByUserId(Long userId) {
        return storage.values().stream()
                .filter(transaction -> userId.equals(transaction.getUserId()))
                .toList();
    }

    public void deleteById(Long id) {
        storage.remove(id);
    }

    public void deleteByUserId(Long userId) {
        storage.values().removeIf(transaction -> userId.equals(transaction.getUserId()));
    }
}
