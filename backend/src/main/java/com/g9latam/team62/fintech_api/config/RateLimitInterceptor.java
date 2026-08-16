package com.g9latam.team62.fintech_api.config;

import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;


@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    // Caché usando Caffeine con expiración automática y tamaño máximo para prevenir agotamiento de memoria (ataques DoS)
    private final Cache<String, Bucket> cache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10)) // Evita que crezca indefinidamente eliminando IPs inactivas tras 10 minutos
            .maximumSize(10000) // Limita el consumo de memoria permitiendo un máximo de 10,000 IPs concurrentes
            .build();

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) throws Exception {
        // Las peticiones preflight OPTIONS no deben verse afectadas por el límite de peticiones (CORS)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // Resuelve de forma segura la IP del cliente. Para prevenir suplantación de cabeceras en producción detrás de
        // proxies inversos (Nginx, AWS, Cloudflare, etc.), se debe configurar `server.forward-headers-strategy=framework`
        // en application.properties. Spring Boot validará X-Forwarded-For de forma segura.
        String ip = request.getRemoteAddr();
        String uri = request.getRequestURI();
        RateLimitPolicy policy = resolvePolicy(uri);
        String cacheKey = ip + ":" + policy.name();
        
        Bucket bucket = cache.get(cacheKey, key -> Bucket.builder()
                .addLimit(policy.getLimit())
                .build());

        if (bucket.tryConsume(1)) {
            return true;
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\": \"Has excedido el límite de peticiones. Intenta más tarde.\"}");
            return false;
        }
    }

    private RateLimitPolicy resolvePolicy(String uri) {
        if (uri.startsWith("/api/auth/login")) {
            return RateLimitPolicy.LOGIN;
        } else if (uri.startsWith("/api/transactions/upload-statement")) {
            return RateLimitPolicy.HEAVY;
        } else if (uri.startsWith("/api/transactions")) {
            return RateLimitPolicy.TRANSACTION_HISTORY;
        } else if (uri.startsWith("/api/auth/register")) {
            return RateLimitPolicy.REGISTER;
        } else if (uri.startsWith("/api/users/change-password")) {
            return RateLimitPolicy.RECOVER_PASSWORD;
        } else {
            return RateLimitPolicy.DEFAULT;
        }
    }
}