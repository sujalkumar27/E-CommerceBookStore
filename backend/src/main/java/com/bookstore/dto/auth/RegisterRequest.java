package com.bookstore.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * ============================================================
 * RegisterRequest — What the client sends to create an account
 * ============================================================
 *
 * WHAT IS A DTO?
 * DTO = Data Transfer Object.
 * It is the shape of data we RECEIVE from the frontend or SEND back.
 * It is NOT the database entity — it only contains what the API needs.
 *
 * WHY NOT USE THE User ENTITY DIRECTLY?
 * The User entity has internal fields (passwordHash, createdAt, etc.)
 * that should never be exposed or accepted from outside.
 * DTOs act as a clean boundary between the API and the database.
 *
 * THIS DTO IS USED FOR:
 *   POST /api/auth/register
 *   Body: { "email": "alice@test.com", "password": "mypassword123" }
 *
 * VALIDATION ANNOTATIONS:
 * @NotBlank  → field must not be null or empty string
 * @Email     → must be a valid email format
 * @Size      → must meet length requirements
 * These are checked automatically when @Valid is used in the controller.
 *
 * Java "record" = an immutable data class.
 * Automatically generates: constructor, getters, equals, hashCode, toString.
 */
public record RegisterRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email address")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password

) {}
