package com.g9latam.team62.fintech_api.service;

import com.g9latam.team62.fintech_api.model.Category;

/**
 * Predicción retornada por el servicio de inferencia de Machine Learning.
 *
 * @param category   la categoría predicha por el modelo
 * @param confidence probabilidad asignada por el modelo (0.0 – 1.0)
 */
public record MlPrediction(
        Category category,
        double confidence) {
}
