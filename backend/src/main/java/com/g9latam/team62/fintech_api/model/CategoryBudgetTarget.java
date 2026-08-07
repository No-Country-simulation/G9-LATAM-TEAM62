package com.g9latam.team62.fintech_api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryBudgetTarget {
    private Long id;
    private Category category;
    private BigDecimal targetPercentage;
    private String countryCode = "CL";
    private String description;

    public BigDecimal getRecommendedPercentage() {
        return targetPercentage;
    }
}
