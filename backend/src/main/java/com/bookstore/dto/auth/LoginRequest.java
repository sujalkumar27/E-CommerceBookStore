package com.bookstore.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * ============================================================
 * LoginRequest — What the client sends to log in
 * ============================================================
 *
 * USED FOR:
 *   POST /api/auth/login
 *   Body: { "email": "alice@test.com", "password": "mypassword123" }
 */
public record LoginRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email address")
        String email,

        @NotBlank(message = "Password is required")
        String password

) {}
