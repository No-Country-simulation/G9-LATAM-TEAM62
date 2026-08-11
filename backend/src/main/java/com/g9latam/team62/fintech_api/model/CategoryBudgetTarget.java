package com.g9latam.team62.fintech_api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;

@Entity
@Table(name = "category_budget_targets")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryBudgetTarget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    private Category category;
    private BigDecimal targetPercentage;
    private String countryCode = "CL";
    private String description;

    public BigDecimal getRecommendedPercentage() {
        return targetPercentage;
    }
}
