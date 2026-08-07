package com.g9latam.team62.fintech_api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinancialProfileHistory {
    private Long id;
    private Long userId;
    private FinancialProfile financialProfile;
    private Double profileAccuracy;
    private LocalDateTime createdAt = LocalDateTime.now();
}
