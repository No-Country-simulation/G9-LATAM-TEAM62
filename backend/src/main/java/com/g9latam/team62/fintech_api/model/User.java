package com.g9latam.team62.fintech_api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
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
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// "user" is a reserved word in Oracle, so the table is named "users"
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    // login identifier, unique across users
    @NotBlank
    @Email
    @Column(nullable = false)
    private String email;

    // stored as a BCrypt hash; write-only so it never appears in responses
    @NotBlank
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String password;

    @PositiveOrZero
    @Column(name = "monthly_income", precision = 19, scale = 2)
    private BigDecimal monthlyIncome;

    @Enumerated(EnumType.STRING)
    @Column(name = "saving_frequency", length = 20)
    private SavingFrequency savingFrequency;

    // financialProfile, profileAccuracy and profileUpdatedAt are written by the
    // external profiling app through PUT /api/users/{id}/profile, not by API clients
    @Enumerated(EnumType.STRING)
    @Column(name = "financial_profile", length = 20)
    private FinancialProfile financialProfile;

    // confidence of the assigned profile, 0.0 to 1.0; null until first computed.
    // Double y no BigDecimal a propósito: la columna profile_accuracy es FLOAT y el perfil
    // oracle arranca con ddl-auto=validate, así que un BigDecimal (que Hibernate espera como
    // number(38,2)) impide que la aplicación levante contra la Autonomous Database.
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    @Column(name = "profile_accuracy")
    private Double profileAccuracy;

    @Column(name = "profile_updated_at")
    private LocalDateTime profileUpdatedAt;

    /**
     * Identidad por clave primaria, no por contenido.
     *
     * <p>Con el {@code @Data} de Lombok, {@code equals} y {@code hashCode} miraban todos los campos:
     * dos filas distintas con los mismos valores se confundían, el hash de una entidad cambiaba al
     * editarla —lo que la vuelve irrecuperable dentro de un {@code HashSet}— y comparar dos referencias
     * podía disparar la carga de relaciones. Una entidad sin id todavía no es nadie, así que solo es
     * igual a sí misma.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof User otro)) {
            return false;
        }
        return id != null && id.equals(otro.getId());
    }

    @Override
    public int hashCode() {
        return User.class.hashCode();
    }
}
