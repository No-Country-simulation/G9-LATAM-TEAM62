package com.g9latam.team62.fintech_api.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;

/**
 * Una palabra clave asociada a una {@link Category}.
 *
 * Se almacena en la tabla {@code category_keywords} y se carga al iniciar la aplicación.
 * Utilizada por el Nivel 2 de la jerarquía de clasificación para comparar las
 * descripciones de transacciones contra palabras clave conocidas en la base de datos,
 * reemplazando el antiguo enum fijo para poder gestionar reglas sin necesidad de redesplegar.
 */
@Entity
@Table(name = "category_keywords")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** La palabra clave a coincidir (almacenada en mayúsculas y formato normalizado). */
    private String keyword;

    /** La categoría a la que pertenece esta palabra clave. */
    @Enumerated(EnumType.STRING)
    private Category category;

    // Identidad por clave primaria; el porqué está en User.equals.
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CategoryKeyword otro)) {
            return false;
        }
        return id != null && id.equals(otro.getId());
    }

    @Override
    public int hashCode() {
        return CategoryKeyword.class.hashCode();
    }
}
