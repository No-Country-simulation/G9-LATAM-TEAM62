package com.g9latam.team62.fintech_api.repository;

import com.g9latam.team62.fintech_api.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserId(Long userId);

    void deleteByUserId(Long userId);

    // usado por BudgetRecommendationService para acotar el análisis a un período
    // (ej. últimos 30 días) en vez de traer todo el historial del usuario
    List<Transaction> findByUserIdAndDateBetween(Long userId, LocalDate start, LocalDate end);
}
