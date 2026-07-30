package com.g9latam.team62.fintech_api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "recommendations", indexes = @Index(name = "idx_recommendations_user_id", columnList = "user_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 1000)
    private String text;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    // profile the user had when this recommendation was generated, so it can be
    // explained or invalidated after the profile changes
    @Enumerated(EnumType.STRING)
    @Column(name = "profile_at_generation", length = 20)
    private FinancialProfile profileAtGeneration;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private Long userId;
}
