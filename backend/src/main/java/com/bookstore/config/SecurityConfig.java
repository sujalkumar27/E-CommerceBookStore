package com.bookstore.config;

import com.bookstore.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * ============================================================
 * SecurityConfig — The Application's Security Rules
 * ============================================================
 *
 * WHAT THIS DOES:
 * This is the "bouncer" configuration for our application.
 * It defines:
 *   1. Which API endpoints are PUBLIC (anyone can call them)
 *   2. Which API endpoints are PROTECTED (only logged-in users)
 *   3. How passwords are hashed (BCrypt)
 *   4. That we use JWT tokens instead of server-side sessions
 *
 * PUBLIC ENDPOINTS (no login needed):
 *   - POST /api/auth/register  → create a new account
 *   - POST /api/auth/login     → log in
 *   - GET  /api/books/**       → browse books (guests can see the catalogue)
 *   - GET  /api/categories/**  → see categories
 *   - GET  /actuator/health    → health check
 *
 * PROTECTED ENDPOINTS (must have a valid JWT token):
 *   - Everything else (basket, orders, payment, addresses, etc.)
 */
@Configuration
@EnableWebSecurity   // Activates Spring Security
@RequiredArgsConstructor  // Lombok: auto-generates a constructor for the final fields below
public class SecurityConfig {

    // Our JWT filter — checks the token on every request
    private final JwtAuthFilter jwtAuthFilter;

    // Spring uses this to load a user from the database by their email
    private final UserDetailsService userDetailsService;

    /**
     * The main security filter chain — defines all access rules.
     * Think of this as writing the rules for who can enter which door.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF (Cross-Site Request Forgery) protection
            // We don't need it because we use JWT tokens, not browser cookies
            .csrf(AbstractHttpConfigurer::disable)

            // Define which routes are public and which need a token
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()                   // Login & register — always public
                .requestMatchers(HttpMethod.GET, "/api/books/**").permitAll()  // Browse books — public (guests allowed)
                .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll() // Browse categories — public
                .requestMatchers("/actuator/health").permitAll()               // Health check — public
                .anyRequest().authenticated()                                  // Everything else: must be logged in
            )

            // Use STATELESS sessions — no server-side session storage
            // The JWT token in each request is the only proof of identity
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Use our custom auth provider (with BCrypt password checking)
            .authenticationProvider(authenticationProvider())

            // Run our JWT filter BEFORE Spring's default login filter
            // So every request is checked for a valid token first
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * AuthenticationProvider — how Spring verifies a user's credentials.
     * It uses our UserDetailsService (loads user from DB) + BCrypt (compares password).
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);  // Load user by email from DB
        provider.setPasswordEncoder(passwordEncoder());      // Compare passwords using BCrypt
        return provider;
    }

    /**
     * Password encoder — BCrypt with strength 12.
     * Strength 12 means the hashing takes ~300ms — slow enough to frustrate
     * brute-force attacks, fast enough that real users don't notice.
     * We NEVER store plain-text passwords — always the BCrypt hash.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * AuthenticationManager — used by our AuthService to verify
     * email + password during login.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
