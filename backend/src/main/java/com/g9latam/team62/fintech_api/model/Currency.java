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
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "currencies",
        uniqueConstraints = @UniqueConstraint(name = "uk_currencies_name", columnNames = "name_currency"))
@Data
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
}
