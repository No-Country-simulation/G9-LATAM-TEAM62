package com.g9latam.team62.fintech_api.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
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
@Getter
@Setter
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

    // Identidad por clave primaria; el porqué está en User.equals.
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CategoryBudgetTarget otro)) {
            return false;
        }
        return id != null && id.equals(otro.getId());
    }

    @Override
    public int hashCode() {
        return CategoryBudgetTarget.class.hashCode();
    }
}
