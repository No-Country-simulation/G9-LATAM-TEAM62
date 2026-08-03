package com.g9latam.team62.fintech_api.repository;

import com.g9latam.team62.fintech_api.model.CategoryMapping;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Repositorio en memoria para entidades {@link CategoryMapping}.
 * Sigue la misma convención que los repositorios existentes en el proyecto
 * (respaldado por ConcurrentHashMap, sin JPA por el momento).
 */
@Repository
public class CategoryMappingRepository {

    private final Map<Long, CategoryMapping> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong();

    public CategoryMapping save(CategoryMapping mapping) {
        if (mapping.getId() == null) {
            mapping.setId(idGenerator.incrementAndGet());
        }
        storage.put(mapping.getId(), mapping);
        return mapping;
    }

    public Optional<CategoryMapping> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    /**
     * Busca un mapeo cuyo {@code descriptionPattern} coincida exactamente
     * con la descripción normalizada provista.
     */
    public Optional<CategoryMapping> findByDescriptionPattern(String descriptionPattern) {
        return storage.values().stream()
                .filter(m -> descriptionPattern.equals(m.getDescriptionPattern()))
                .findFirst();
    }

    public Collection<CategoryMapping> findAll() {
        return storage.values();
    }

    public void deleteById(Long id) {
        storage.remove(id);
    }
}
