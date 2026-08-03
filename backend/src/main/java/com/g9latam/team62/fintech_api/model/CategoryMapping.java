package com.g9latam.team62.fintech_api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representa un mapeo aprendido entre un patrón de descripción normalizado
 * de transacción y una categoría.
 * Se construye mediante retroalimentación de usuarios (crowdsourcing):
 * cada vez que un usuario corrige una categoría, el mapeo se crea o actualiza
 * para que futuras transacciones con la misma descripción se clasifiquen
 * instantáneamente (Nivel 1 de la jerarquía de clasificación).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryMapping {

    private Long id;

    /** Descripción normalizada (en mayúsculas, sin tildes, sin números). */
    private String descriptionPattern;

    /** Categoría asociada a este patrón. */
    private Category category;

    /** Cantidad de veces que los usuarios han confirmado o establecido este mapeo. */
    private int frequency;

    public CategoryMapping(String descriptionPattern, Category category, int frequency) {
        this.descriptionPattern = descriptionPattern;
        this.category = category;
        this.frequency = frequency;
    }
}
