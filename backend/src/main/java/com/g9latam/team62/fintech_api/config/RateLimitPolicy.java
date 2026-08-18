package com.g9latam.team62.fintech_api.config;

import java.time.Duration;

import io.github.bucket4j.Bandwidth;

public enum RateLimitPolicy {
    // Por encima de LoginAttemptService.MAX_ATTEMPTS (5) a propósito. Con ambos límites en 5,
    // el cubo por IP se agotaba en el mismo intento en que debía activarse el bloqueo por
    // cuenta, así que el bloqueo de 15 minutos nunca llegaba a aplicarse. Este margen deja
    // que actúe primero la defensa específica y la de IP siga cubriendo el resto.
    LOGIN(10, Duration.ofMinutes(10)),
    REGISTER(2, Duration.ofHours(1)),
    RECOVER_PASSWORD(3, Duration.ofHours(1)),
    TRANSACTION_HISTORY(100, Duration.ofHours(1)),
    HEAVY(10, Duration.ofMinutes(1)),
    DEFAULT(100, Duration.ofMinutes(1));

    private final int capacity;
    private final Duration duration;

    RateLimitPolicy(int capacity, Duration duration) {
        this.capacity = capacity;
        this.duration = duration;
    }

    public Bandwidth getLimit() {
        return Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, duration)
                .build();
    }
}