package com.g9latam.team62.fintech_api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Una palabra clave asociada a una {@link Category}.
 *
 * Se almacena en la tabla {@code category_keywords} y se carga al iniciar la aplicación.
 * Utilizada por el Nivel 2 de la jerarquía de clasificación para comparar las
 * descripciones de transacciones contra palabras clave conocidas en la base de datos,
 * reemplazando el antiguo enum fijo para poder gestionar reglas sin necesidad de redesplegar.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryKeyword {

    private Long id;

    /** La palabra clave a coincidir (almacenada en mayúsculas y formato normalizado). */
    private String keyword;

    /** La categoría a la que pertenece esta palabra clave. */
    private Category category;
}
