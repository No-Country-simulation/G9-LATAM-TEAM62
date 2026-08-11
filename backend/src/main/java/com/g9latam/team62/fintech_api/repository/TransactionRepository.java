package com.g9latam.team62.fintech_api.repository;

import com.g9latam.team62.fintech_api.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserId(Long userId);
    List<Transaction> findByUserIdAndDateBetween(Long userId, java.time.LocalDate start, java.time.LocalDate end);
    void deleteByUserId(Long userId);
}
