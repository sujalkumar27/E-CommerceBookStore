package com.bookstore.config;

import com.bookstore.security.JwtAuthFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
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
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.Instant;
import java.util.Map;

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
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

            // ── Custom error responses ──────────────────────────────────────────
            //
            // WHY THIS IS NEEDED:
            // By default, Spring Security's ExceptionTranslationFilter returns its own
            // HTML error page (or a raw 403) when authentication/authorisation fails.
            // This bypasses our GlobalExceptionHandler entirely, so the frontend gets
            // a 403 with no JSON body instead of the expected 401/403 JSON error.
            //
            // FIX:
            //   authenticationEntryPoint — called when a request has NO valid credentials
            //     (anonymous user hits a protected route) → should return 401 Unauthorized
            //   accessDeniedHandler — called when a request HAS valid credentials but
            //     lacks permission for the resource → should return 403 Forbidden
            //
            // Both handlers write a clean JSON error body so the frontend can parse it.
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint())
                .accessDeniedHandler(accessDeniedHandler())
            );

        return http.build();
    }

    /**
     * AuthenticationEntryPoint — invoked when an unauthenticated request reaches a
     * protected endpoint (no token, or token validation failed before reaching the
     * controller).  Returns HTTP 401 with a JSON body matching our ErrorResponse shape.
     *
     * Without this, Spring Security writes its own redirect / HTML 403 page,
     * which the frontend cannot parse.
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);           // 401
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            new ObjectMapper().writeValue(response.getWriter(), Map.of(
                    "status",    401,
                    "error",     "Unauthorized",
                    "message",   "Authentication required. Please log in.",
                    "timestamp", Instant.now().toString(),
                    "path",      request.getRequestURI()
            ));
        };
    }

    /**
     * AccessDeniedHandler — invoked when an authenticated user tries to access a
     * resource they do not have permission for.  Returns HTTP 403 with a JSON body.
     *
     * Also covers the case where Spring Security's ExceptionTranslationFilter
     * intercepts a BadCredentialsException and would otherwise silently return 403.
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);              // 403
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            new ObjectMapper().writeValue(response.getWriter(), Map.of(
                    "status",    403,
                    "error",     "Forbidden",
                    "message",   "You do not have permission to access this resource.",
                    "timestamp", Instant.now().toString(),
                    "path",      request.getRequestURI()
            ));
        };
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
