package com.g9latam.team62.fintech_api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// % de referencia que una categoría de gasto debería representar sobre el
// total de gastos (type = EXPENSE) de un usuario, por país. Ver
// infra/oci/category_budget_targets.sql para la carga inicial (Chile, INE).
@Entity
@Table(name = "category_budget_targets",
        uniqueConstraints = @UniqueConstraint(name = "uk_cbt_country_category",
                columnNames = {"country_code", "category"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryBudgetTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Category category;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("100.0")
    @Column(name = "recommended_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal recommendedPercentage;

    @Column(length = 255)
    private String source;
}
