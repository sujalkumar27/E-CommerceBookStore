package com.bookstore.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * ============================================================
 * CorsConfig — Cross-Origin Resource Sharing Configuration
 * ============================================================
 *
 * PROBLEM THIS SOLVES:
 * By default, browsers block a webpage from calling an API on a
 * DIFFERENT domain/port. Our React frontend runs on port 5173
 * and our backend runs on port 8080 — that's two different "origins".
 * Without CORS config, the browser would block every API call.
 *
 * WHAT THIS DOES:
 * It tells the browser: "It's okay for http://localhost:5173
 * (our React app) to call http://localhost:8080 (our backend)."
 *
 * SECURITY NOTE:
 * We only allow our specific frontend origin — NOT every website.
 * This is controlled by the FRONTEND_ORIGIN environment variable.
 */
@Configuration  // Tells Spring: this class contains configuration settings
public class CorsConfig implements WebMvcConfigurer {

    // Read the allowed frontend URL from environment variable
    // Default: http://localhost:5173 (where Vite runs React in dev mode)
    @Value("${app.cors.frontend-origin}")
    private String frontendOrigin;

    /**
     * Configure which origins (websites) are allowed to call our API.
     *
     * @param registry - Spring's CORS registry where we add our rules
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")            // Apply to all /api/* routes
                .allowedOrigins(frontendOrigin)   // Only allow our React frontend
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")              // Allow any header (including Authorization)
                .allowCredentials(true)           // Allow cookies/auth headers
                .maxAge(3600);                    // Cache CORS check for 1 hour
    }
}
