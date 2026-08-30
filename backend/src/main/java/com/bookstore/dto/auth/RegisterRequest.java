package com.bookstore.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * ============================================================
 * RegisterRequest — What the client sends to create an account
 * ============================================================
 *
 * FIELDS:
 *   name     — user's display name (shown in the UI)
 *   email    — login email (must be unique)
 *   password — plain-text password (hashed by BCrypt before storing)
 *
 * VALIDATION ANNOTATIONS:
 * @NotBlank  → field must not be null or empty string
 * @Email     → must be a valid email format
 * @Size      → must meet length requirements
 * These are checked automatically when @Valid is used in the controller.
 */
public record RegisterRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be 100 characters or fewer")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email address")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password

) {}
