package com.g9latam.team62.fintech_api.repository;

import com.g9latam.team62.fintech_api.model.CategoryMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryMappingRepository extends JpaRepository<CategoryMapping, Long> {
    Optional<CategoryMapping> findByDescriptionPattern(String descriptionPattern);
}
