package com.g9latam.team62.fintech_api.repository;

import com.g9latam.team62.fintech_api.model.Category;
import com.g9latam.team62.fintech_api.model.CategoryKeyword;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Repositorio en memoria para entidades {@link CategoryKeyword}.
 *
 * Al inicializarse el componente, lee y parsea las sentencias INSERT contenidas en {@code schema.sql}
 * para cargar automáticamente las palabras clave iniciales en la base de datos.
 * Se pueden agregar o modificar palabras clave en tiempo de ejecución sin reiniciar la aplicación.
 */
@Repository
public class CategoryKeywordRepository {

    private final Map<Long, CategoryKeyword> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong();

    private static final Pattern INSERT_VAL_PATTERN = Pattern.compile("\\('([^']+)',\\s*'([^']+)'\\)");

    @PostConstruct
    public void initFromSchemaSql() {
        try {
            ClassPathResource resource = new ClassPathResource("schema.sql");
            if (!resource.exists()) {
                return;
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher matcher = INSERT_VAL_PATTERN.matcher(line);
                    while (matcher.find()) {
                        String keyword = matcher.group(1).trim();
                        String categoryStr = matcher.group(2).trim();
                        try {
                            Category category = Category.valueOf(categoryStr);
                            save(new CategoryKeyword(null, keyword, category));
                        } catch (IllegalArgumentException ignored) {
                            // Ignora categorías que no coincidan con el Enum
                        }
                    }
                }
            }
            System.out.println(">>> SE CARGARON " + storage.size() + " PALABRAS CLAVE DESDE schema.sql");
        } catch (Exception e) {
            System.err.println(">>> Error al cargar palabras clave desde schema.sql: " + e.getMessage());
        }
    }

    public CategoryKeyword save(CategoryKeyword keyword) {
        if (keyword.getId() == null) {
            keyword.setId(idGenerator.incrementAndGet());
        }
        storage.put(keyword.getId(), keyword);
        return keyword;
    }

    public Optional<CategoryKeyword> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    public Collection<CategoryKeyword> findAll() {
        return storage.values();
    }

    /**
     * Busca la primera palabra clave contenida dentro de la descripción normalizada provista.
     *
     * @param normalizedDescription el texto normalizado sobre el cual buscar
     * @return la {@link Category} correspondiente si coincide alguna palabra clave
     */
    public Optional<Category> matchDescription(String normalizedDescription) {
        for (CategoryKeyword kw : storage.values()) {
            if (normalizedDescription.contains(kw.getKeyword())) {
                return Optional.of(kw.getCategory());
            }
        }
        return Optional.empty();
    }

    public void deleteById(Long id) {
        storage.remove(id);
    }
}
