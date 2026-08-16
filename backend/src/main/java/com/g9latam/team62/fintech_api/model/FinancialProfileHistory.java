package com.g9latam.team62.fintech_api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinancialProfileHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long userId;
    
    @Enumerated(EnumType.STRING)
    private FinancialProfile financialProfile;
    private BigDecimal profileAccuracy;
    private LocalDateTime createdAt = LocalDateTime.now();
}
