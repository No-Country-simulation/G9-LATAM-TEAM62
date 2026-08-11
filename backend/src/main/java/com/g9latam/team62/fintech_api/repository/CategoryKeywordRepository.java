package com.g9latam.team62.fintech_api.repository;

import com.g9latam.team62.fintech_api.model.Category;
import com.g9latam.team62.fintech_api.model.CategoryKeyword;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryKeywordRepository extends JpaRepository<CategoryKeyword, Long> {

    /**
     * Busca la primera palabra clave contenida dentro de la descripción normalizada provista.
     *
     * @param normalizedDescription el texto normalizado sobre el cual buscar
     * @return la {@link Category} correspondiente si coincide alguna palabra clave
     */
    default Optional<Category> matchDescription(String normalizedDescription) {
        for (CategoryKeyword kw : findAll()) {
            if (normalizedDescription.contains(kw.getKeyword())) {
                return Optional.of(kw.getCategory());
            }
        }
        return Optional.empty();
    }
}
