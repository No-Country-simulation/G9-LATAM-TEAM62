package com.g9latam.team62.fintech_api.repository;

import com.g9latam.team62.fintech_api.model.FinancialProfileHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinancialProfileHistoryRepository extends JpaRepository<FinancialProfileHistory, Long> {
    List<FinancialProfileHistory> findByUserIdOrderByCreatedAtDesc(Long userId);
}
