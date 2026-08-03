package com.g9latam.team62.fintech_api.service;

import com.g9latam.team62.fintech_api.dto.MlPrediction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Servicio que delega al modelo de Machine Learning entrenado por el equipo de Datos
 * (Nivel 3 de la jerarquía de clasificación).
 *
 * Actualmente es una estructura preparada (stub): {@link #isAvailable()}
 * retorna {@code false} hasta que el equipo de Datos despliegue su modelo y
 * se configure {@code ml.inference.enabled=true} en application.properties.
 */
@Service
public class MlInferenceService {

    @Value("${ml.inference.enabled:false}")
    private boolean enabled;

    @Value("${ml.inference.url:}")
    private String inferenceUrl;

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

        // Pendiente de conectar con el endpoint HTTP/gRPC del modelo entrenado
        throw new UnsupportedOperationException(
                "Integración con modelo ML no implementada aún. "
              + "A la espera del despliegue del endpoint por parte del equipo de Datos.");
    }
}
