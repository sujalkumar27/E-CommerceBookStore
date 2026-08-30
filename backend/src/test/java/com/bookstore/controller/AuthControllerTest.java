package com.bookstore.controller;

import com.bookstore.dto.auth.AuthResponse;
import com.bookstore.exception.ConflictException;
import com.bookstore.exception.GlobalExceptionHandler;
import com.bookstore.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ============================================================
 * AuthControllerTest — Integration Tests for Auth Endpoints
 * ============================================================
 *
 * WHAT WE ARE TESTING (HTTP layer):
 *   POST /api/auth/register → 201 / 400 / 409
 *   POST /api/auth/login    → 200 / 400
 *   POST /api/auth/logout   → 200
 *
 * @Import(TestSecurityConfig.class) — replaces production SecurityConfig with
 * a test version that permits all requests and disables CSRF.
 * This avoids JwtAuthFilter bean-conflict issues in @WebMvcTest.
 */
@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest extends BaseControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean  AuthService authService;

    private static final String EMAIL = "alice@test.com";
    private static final String TOKEN = "eyJhbGci.fake.token";

    private AuthResponse buildAuthResponse() {
        return new AuthResponse(TOKEN,
                new AuthResponse.UserInfo(UUID.randomUUID(), "Test User", EMAIL, 0, Instant.now()));
    }

    // ─────────────────────────────────────────────────────────
    // POST /api/auth/register
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /register: valid body → 201 Created with token")
    void register_validRequest_returns201() throws Exception {
        when(authService.register(any())).thenReturn(buildAuthResponse());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Alice", "email": "alice@test.com", "password": "password123" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value(TOKEN))
                .andExpect(jsonPath("$.user.email").value(EMAIL));
    }

    @Test
    @DisplayName("POST /register: missing email → 400 with field error")
    void register_missingEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Alice", "password": "password123" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    @DisplayName("POST /register: password too short → 400 with field error")
    void register_shortPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Alice", "email": "alice@test.com", "password": "short" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    @DisplayName("POST /register: invalid email format → 400 with field error")
    void register_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Alice", "email": "not-an-email", "password": "password123" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    @DisplayName("POST /register: duplicate email → 409 Conflict")
    void register_duplicateEmail_returns409() throws Exception {
        when(authService.register(any()))
                .thenThrow(new ConflictException("An account with this email already exists."));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Alice", "email": "alice@test.com", "password": "password123" }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    // ─────────────────────────────────────────────────────────
    // POST /api/auth/login
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /login: valid credentials → 200 OK with token")
    void login_validRequest_returns200() throws Exception {
        when(authService.login(any())).thenReturn(buildAuthResponse());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "alice@test.com", "password": "password123" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(TOKEN))
                .andExpect(jsonPath("$.user.email").value(EMAIL));
    }

    @Test
    @DisplayName("POST /login: missing password → 400 Bad Request")
    void login_missingPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "alice@test.com" }
                                """))
                .andExpect(status().isBadRequest());
    }

    // ─────────────────────────────────────────────────────────
    // POST /api/auth/logout
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /logout: always 200 OK")
    void logout_returns200WithMessage() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully."));
    }
}
