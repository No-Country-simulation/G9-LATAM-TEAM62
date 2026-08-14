package com.g9latam.team62.fintech_api.repository;

import com.g9latam.team62.fintech_api.model.CategoryBudgetTarget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryBudgetTargetRepository extends JpaRepository<CategoryBudgetTarget, Long> {

    List<CategoryBudgetTarget> findByCountryCode(String countryCode);
}
