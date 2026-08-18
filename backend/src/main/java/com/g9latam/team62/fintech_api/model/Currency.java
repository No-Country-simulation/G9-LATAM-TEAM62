package com.g9latam.team62.fintech_api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "currencies",
        uniqueConstraints = @UniqueConstraint(name = "uk_currencies_name", columnNames = "name_currency"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Currency {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // the JSON name stays "name_currency" so existing clients keep working
    @NotBlank
    @JsonProperty("name_currency")
    @Column(name = "name_currency", nullable = false, length = 100)
    private String nameCurrency;

    // Identidad por clave primaria; el porqué está en User.equals.
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Currency otro)) {
            return false;
        }
        return id != null && id.equals(otro.getId());
    }

    @Override
    public int hashCode() {
        return Currency.class.hashCode();
    }
}
