package com.g9latam.team62.fintech_api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @NonNull
    private final RateLimitInterceptor rateLimitInterceptor;

    // Se usa inyección por constructor en lugar de @Autowired sobre el campo (buena práctica recomendada)
    public WebMvcConfig(@NonNull RateLimitInterceptor rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        // Aplica el límite solo a los endpoints de la API
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/api/**");
    }
}