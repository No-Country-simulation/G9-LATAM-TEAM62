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
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Una fila por cada vez que la app externa de perfilado escribe
// financial_profile en `users` (vía PUT /api/users/{id}/profile). Permite
// reconstruir la evolución del perfil financiero de un usuario en el tiempo,
// algo que `users` por sí solo no puede hacer porque se sobreescribe.
@Entity
@Table(name = "financial_profile_history",
        indexes = @Index(name = "idx_fph_user_id", columnList = "user_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinancialProfileHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "financial_profile", nullable = false, length = 20)
    private FinancialProfile financialProfile;

    @Column(name = "profile_accuracy")
    private Double profileAccuracy;

    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;
}
