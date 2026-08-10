package com.g9latam.team62.fintech_api.repository;

import com.g9latam.team62.fintech_api.model.FinancialProfileHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinancialProfileHistoryRepository extends JpaRepository<FinancialProfileHistory, Long> {

    // orden ascendente por fecha: pensado para graficar la evolución tal cual,
    // sin que el llamador tenga que ordenar de nuevo
    List<FinancialProfileHistory> findByUserIdOrderByRecordedAtAsc(Long userId);

    void deleteByUserId(Long userId);
}
