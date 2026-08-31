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

    // Read the allowed frontend URL from environment variable.
    // Default: http://localhost:5173 (where Vite runs React in dev mode).
    //
    // ROOT CAUSE OF 403 ON REGISTER/LOGIN:
    // Vite's server is bound to host 127.0.0.1 (not localhost).
    // The browser therefore sends Origin: http://127.0.0.1:5173.
    // If CorsConfig only allows http://localhost:5173, the CORS filter
    // rejects the request with 403 BEFORE it reaches Spring Security
    // or any controller — so the custom AuthenticationEntryPoint never runs.
    //
    // FIX: always allow BOTH http://localhost:5173 AND http://127.0.0.1:5173
    // so the Vite dev server works regardless of which hostname the browser uses.
    @Value("${app.cors.frontend-origin}")
    private String frontendOrigin;

    /**
     * Configure which origins (websites) are allowed to call our API.
     *
     * @param registry - Spring's CORS registry where we add our rules
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Build the list of allowed origins.
        // Always include both http://localhost:5173 and http://127.0.0.1:5173
        // because Vite binds to 127.0.0.1 but browsers may send either hostname.
        String[] allowed = buildAllowedOrigins(frontendOrigin);

        registry.addMapping("/api/**")       // Apply to all /api/* routes
                .allowedOrigins(allowed)     // Only allow our React frontend (both variants)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")         // Allow any header (including Authorization)
                .allowCredentials(true)      // Allow cookies/auth headers
                .maxAge(3600);               // Cache CORS preflight for 1 hour
    }

    /**
     * Given a configured origin (e.g. "http://localhost:5173"), returns an array
     * that also includes the 127.0.0.1 and localhost variants so Vite works
     * regardless of which form the browser uses for the Origin header.
     */
    private String[] buildAllowedOrigins(String configured) {
        // Always include the configured value plus both localhost forms on port 5173
        return new String[] {
            configured,
            "http://localhost:5173",
            "http://127.0.0.1:5173"
        };
    }
}
