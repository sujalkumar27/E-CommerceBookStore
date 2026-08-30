package com.bookstore.controller;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * ============================================================
 * TestSecurityConfig — Minimal Security for @WebMvcTest
 * ============================================================
 *
 * WHY THIS EXISTS:
 * @WebMvcTest loads the Spring Security filter chain automatically.
 * Importing the real SecurityConfig causes bean conflicts because it
 * instantiates the real JwtAuthFilter, which conflicts with the
 * @MockBean version declared in BaseControllerTest.
 *
 * This test-only config replaces the real SecurityConfig.
 * It disables CSRF and permits all requests, so every test
 * can control the auth state itself using .with(user(...)).
 *
 * Controller tests verify business logic routing and HTTP status codes.
 * Security rules (who can call what) are exercised in the real app
 * and are already implicitly tested via the 401/403 assertions that
 * use .with(user(alice)) vs no-auth requests.
 *
 * @TestConfiguration — only active in tests, never in production.
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
