package com.g9latam.team62.fintech_api.service;

import java.time.Duration;
import org.springframework.stereotype.Service;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

@Service
public class LoginAttemptService {
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    // Guarda los intentos fallidos por email
    private final Cache<String, Integer> attemptsCache = Caffeine.newBuilder()
            .expireAfterWrite(LOCK_DURATION)
            .build();

    public void loginFailed(String email) {
        int attempts = attemptsCache.get(email.toLowerCase(), key -> 0);
        attemptsCache.put(email.toLowerCase(), attempts + 1);
    }

    public void loginSucceeded(String email) {
        attemptsCache.invalidate(email.toLowerCase());
    }

    public boolean isBlocked(String email) {
        Integer attempts = attemptsCache.getIfPresent(email.toLowerCase());
        return attempts != null && attempts >= MAX_ATTEMPTS;
    }
}

