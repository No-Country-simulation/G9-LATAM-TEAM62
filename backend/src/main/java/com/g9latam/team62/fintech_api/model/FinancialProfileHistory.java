package com.g9latam.team62.fintech_api.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;

@Entity
@Table(name = "financial_profile_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FinancialProfileHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long userId;
    
    @Enumerated(EnumType.STRING)
    private FinancialProfile financialProfile;
    // Double para coincidir con la columna FLOAT; ver la nota en User.profileAccuracy.
    private Double profileAccuracy;
    private LocalDateTime createdAt = LocalDateTime.now();

    // Identidad por clave primaria; el porqué está en User.equals.
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FinancialProfileHistory otro)) {
            return false;
        }
        return id != null && id.equals(otro.getId());
    }

    @Override
    public int hashCode() {
        return FinancialProfileHistory.class.hashCode();
    }
}
