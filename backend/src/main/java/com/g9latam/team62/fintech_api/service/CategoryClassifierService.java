package com.g9latam.team62.fintech_api.service;

import com.g9latam.team62.fintech_api.dto.ClassificationResult;
import com.g9latam.team62.fintech_api.dto.MlPrediction;
import com.g9latam.team62.fintech_api.model.Category;
import com.g9latam.team62.fintech_api.model.CategoryMapping;
import com.g9latam.team62.fintech_api.repository.CategoryKeywordRepository;
import com.g9latam.team62.fintech_api.repository.CategoryMappingRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Clasifica transacciones por descripción en 4 niveles de precisión:
 * 1. Mapeo exacto por usuarios ({@code transaction_category_mappings}).
 * 2. Reglas por palabras clave ({@code category_keywords}).
 * 3. Predicción de ML ({@link MlInferenceService}).
 * 4. Fallback ({@code OTHER_EXPENSE}).
 */
@Service
public class CategoryClassifierService {

    /** Umbral mínimo de confianza requerido del modelo ML para aceptar su predicción. */
    private static final double ML_CONFIDENCE_THRESHOLD = 0.60;

    private final CategoryMappingRepository mappingRepository;
    private final CategoryKeywordRepository keywordRepository;
    private final MlInferenceService mlInferenceService;

    public CategoryClassifierService(CategoryMappingRepository mappingRepository,
                                     CategoryKeywordRepository keywordRepository,
                                     MlInferenceService mlInferenceService) {
        this.mappingRepository = mappingRepository;
        this.keywordRepository = keywordRepository;
        this.mlInferenceService = mlInferenceService;
    }

    /**
     * Clasifica una descripción de transacción bancaria a través de la jerarquía de 4 niveles.
     *
     * @param rawDescription la descripción original de la transacción
     * @return un {@link ClassificationResult} con la categoría asignada, el nivel de confianza y el método
     */
    public ClassificationResult classify(String rawDescription) {
        if (rawDescription == null || rawDescription.isBlank()) {
            return new ClassificationResult(Category.OTHER_EXPENSE, 0.0, "FALLBACK");
        }

        // Paso 0: Normalización del texto
        String normalized = TextNormalizer.normalize(rawDescription);

        if (normalized.isBlank()) {
            return new ClassificationResult(Category.OTHER_EXPENSE, 0.0, "FALLBACK");
        }

        // Nivel 1: Coincidencia exacta en BD por retroalimentación/crowdsourcing
        Optional<CategoryMapping> mapping = mappingRepository.findByDescriptionPattern(normalized);
        if (mapping.isPresent()) {
            return new ClassificationResult(mapping.get().getCategory(), 1.0, "BD_MAPPING");
        }

        // Nivel 2: Coincidencia por palabra clave desde la BD (tabla category_keywords)
        Optional<Category> keywordMatch = keywordRepository.matchDescription(normalized);
        if (keywordMatch.isPresent()) {
            return new ClassificationResult(keywordMatch.get(), 0.9, "KEYWORD_RULE");
        }

        // Nivel 3: Inferencia del modelo de Machine Learning (si está disponible)
        if (mlInferenceService.isAvailable()) {
            try {
                MlPrediction prediction = mlInferenceService.predict(normalized);
                if (prediction.confidence() >= ML_CONFIDENCE_THRESHOLD) {
                    return new ClassificationResult(
                            prediction.category(), prediction.confidence(), "ML_MODEL");
                }
            } catch (Exception e) {
                // Si falla el servicio ML, continúa al fallback sin romper la ejecución
            }
        }

        // Nivel 4: Fallback por defecto
        return new ClassificationResult(Category.OTHER_EXPENSE, 0.0, "FALLBACK");
    }

    /**
     * Registra la corrección de categoría de un usuario para que futuras transacciones
     * con la misma descripción normalizada se clasifiquen instantáneamente (Nivel 1).
     *
     * @param rawDescription       descripción original de la transacción
     * @param userSelectedCategory nueva categoría seleccionada por el usuario
     */
    public void learnFromFeedback(String rawDescription, Category userSelectedCategory) {
        if (rawDescription == null || rawDescription.isBlank() || userSelectedCategory == null) {
            return;
        }

        String normalized = TextNormalizer.normalize(rawDescription);
        if (normalized.isBlank()) {
            return;
        }

        CategoryMapping mapping = mappingRepository.findByDescriptionPattern(normalized)
                .orElse(new CategoryMapping(normalized, userSelectedCategory, 0));

        mapping.setCategory(userSelectedCategory);
        mapping.setFrequency(mapping.getFrequency() + 1);

        mappingRepository.save(mapping);
    }
}
