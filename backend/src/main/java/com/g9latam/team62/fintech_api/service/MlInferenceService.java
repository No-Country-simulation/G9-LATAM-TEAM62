package com.g9latam.team62.fintech_api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.g9latam.team62.fintech_api.dto.MlPrediction;
import com.g9latam.team62.fintech_api.model.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Servicio que delega al modelo de Machine Learning entrenado por el equipo de Datos
 * (Nivel 3 de la jerarquía de clasificación).
 *
 * Se conecta mediante peticiones HTTP REST POST al endpoint del modelo ML (FastAPI, Flask u OCI)
 * cuando se configura {@code ml.inference.enabled=true} en application.properties.
 */
@Service
public class MlInferenceService {

    private static final Logger logger = LoggerFactory.getLogger(MlInferenceService.class);

    @Value("${ml.inference.enabled:false}")
    private boolean enabled;

    @Value("${ml.inference.url:}")
    private String inferenceUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Retorna si el modelo ML está desplegado y listo para recibir peticiones.
     */
    public boolean isAvailable() {
        return enabled && inferenceUrl != null && !inferenceUrl.isBlank();
    }

    /**
     * Envía la descripción normalizada al modelo ML y retorna la predicción.
     *
     * @param normalizedDescription texto procesado por {@link TextNormalizer#normalize}
     * @return predicción del modelo (categoría + nivel de confianza)
     */
    public MlPrediction predict(String normalizedDescription) {
        if (!isAvailable()) {
            throw new UnsupportedOperationException(
                    "El servicio de inferencia ML no está disponible. "
                  + "Configure ml.inference.enabled=true y ml.inference.url en application.properties.");
        }

        try {
            // Payload de petición JSON: {"descripcion": "..."}
            Map<String, String> requestBody = Map.of("descripcion", normalizedDescription);
            String jsonPayload = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(inferenceUrl))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Mapear respuesta JSON: {"category": "FOOD", "confidence": 0.85}
                MlResponseDTO mlResponse = objectMapper.readValue(response.body(), MlResponseDTO.class);
                Category cat = parseCategory(mlResponse.category());
                return new MlPrediction(cat, mlResponse.confidence());
            } else {
                logger.error("Error del modelo de ML. Status code: {}, Body: {}", response.statusCode(), response.body());
                throw new IllegalStateException("Servicio de inferencia de ML retornó código " + response.statusCode());
            }
        } catch (Exception e) {
            logger.error("Fallo al conectar con el servicio de inferencia de ML: {}", e.getMessage());
            throw new RuntimeException("Error en la inferencia de ML: " + e.getMessage(), e);
        }
    }

    private Category parseCategory(String categoryName) {
        if (categoryName == null) return Category.SHOPPING;
        try {
            return Category.valueOf(categoryName.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            // Mapeos y fallbacks flexibles por si la respuesta varía en español/inglés
            String clean = categoryName.trim().toUpperCase();
            if (clean.contains("ALIMENTACION") || clean.contains("FOOD")) return Category.FOOD;
            if (clean.contains("TRANSPORTE") || clean.contains("TRANSPORT")) return Category.TRANSPORT;
            if (clean.contains("SALUD") || clean.contains("HEALTH")) return Category.HEALTH;
            if (clean.contains("VIVIENDA") || clean.contains("HOUSING")) return Category.HOUSING;
            if (clean.contains("EDUCACION") || clean.contains("EDUCATION")) return Category.EDUCATION;
            if (clean.contains("OCIO") || clean.contains("ENTERTAINMENT")) return Category.ENTERTAINMENT;
            if (clean.contains("SERVICIOS") || clean.contains("UTILITIES")) return Category.UTILITIES;
            return Category.SHOPPING;
        }
    }

    // DTO interno para parsear la respuesta JSON del servidor Python
    private static record MlResponseDTO(String category, double confidence) {}
}
