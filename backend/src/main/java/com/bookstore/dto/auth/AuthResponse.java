package com.bookstore.dto.auth;

import java.time.Instant;
import java.util.UUID;

/**
 * ============================================================
 * AuthResponse — What the server sends back after login/register
 * ============================================================
 *
 * USED FOR:
 *   POST /api/auth/register → 201 Created
 *   POST /api/auth/login    → 200 OK
 *
 * EXAMPLE JSON RESPONSE:
 * {
 *   "token": "eyJhbGciOiJIUzI1NiJ9...",
 *   "user": {
 *     "id": "550e8400-e29b-41d4-a716-446655440000",
 *     "email": "alice@test.com",
 *     "giftPointBalance": 0,
 *     "createdAt": "2026-08-29T10:00:00Z"
 *   }
 * }
 *
 * HOW THE FRONTEND USES THIS:
 * 1. Store the "token" in localStorage
 * 2. Attach it to every future request: "Authorization: Bearer <token>"
 * 3. Use "user" data to display the user's name/email in the UI
 *
 * WHY A NESTED "user" OBJECT?
 * Clean structure — token and user info are logically separate.
 * The frontend knows exactly where to find each piece.
 */
public record AuthResponse(
        String token,   // The JWT token — the "login wristband"
        UserInfo user   // Basic user details for the frontend
) {
    /**
     * UserInfo — the user details included in the auth response.
     * Only includes safe fields — NEVER the password hash.
     */
    public record UserInfo(
            UUID id,
            String name,
            String email,
            int giftPointBalance,
            Instant createdAt
    ) {}
}
