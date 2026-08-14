package com.g9latam.team62.fintech_api.config;

import java.time.Duration;

import io.github.bucket4j.Bandwidth;

public enum RateLimitPolicy {
    LOGIN(5, Duration.ofMinutes(10)),
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