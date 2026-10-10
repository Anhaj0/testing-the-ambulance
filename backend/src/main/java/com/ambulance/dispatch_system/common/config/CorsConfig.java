package com.ambulance.dispatch_system.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Single, application-wide CORS policy for every {@code /api/**} endpoint. Replaces the
 * per-controller {@code @CrossOrigin} annotations that used to disagree with each other -
 * some allowed only {@code app.cors.allowed-origin-patterns}, some hardcoded a wildcard, and
 * most controllers had no CORS annotation at all (effectively no cross-origin access unless
 * something else configured it). One policy, one place, configurable per environment.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*}")
    private String[] allowedOriginPatterns;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOriginPatterns)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
