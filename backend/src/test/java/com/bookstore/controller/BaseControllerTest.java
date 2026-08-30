package com.bookstore.controller;

import com.bookstore.model.User;
import com.bookstore.security.JwtAuthFilter;
import com.bookstore.security.JwtUtil;
import com.bookstore.service.UserDetailsServiceImpl;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

/**
 * ============================================================
 * BaseControllerTest — Shared Setup for All @WebMvcTest Classes
 * ============================================================
 *
 * WHAT THIS IS:
 * A base class that every controller integration test extends.
 *
 * WHY @MockBean FOR SECURITY BEANS:
 * @WebMvcTest loads the full Spring Security infrastructure.
 * JwtAuthFilter, JwtUtil, and UserDetailsServiceImpl are components
 * used by SecurityConfig. If they are NOT mocked, Spring tries to
 * auto-wire them — and then tries to load the real database, which
 * is not available in the @WebMvcTest slice. Mocking them prevents
 * that auto-wiring chain from failing.
 *
 * @ActiveProfiles("test") loads application-test.yml, which provides
 * the JWT secret and expiry needed if JwtUtil is constructed anywhere.
 */
@ActiveProfiles("test")
public abstract class BaseControllerTest {

    // Spring Security beans — must be mocked so the context can start
    @MockBean protected JwtAuthFilter jwtAuthFilter;
    @MockBean protected JwtUtil jwtUtil;
    @MockBean protected UserDetailsServiceImpl userDetailsService;

    /**
     * Build a User entity with a random UUID for use as a mock principal.
     * Used with .with(user(alice)) in MockMvc requests.
     */
    protected User buildUser(String email) {
        User u = new User("Test User", email, "$2b$12$fakehash");
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(u, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return u;
    }
}

