package com.bookstore.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ============================================================
 * JwtUtilTest — Unit Tests for JWT Token Logic
 * ============================================================
 *
 * WHAT WE ARE TESTING:
 * The JwtUtil class generates and validates JWT tokens.
 * These tests verify that tokens:
 *   - Are generated and contain the correct email
 *   - Are valid immediately after creation
 *   - Are rejected if they are expired
 *   - Are rejected if they have been tampered with
 *
 * HOW THESE TESTS WORK:
 * JwtUtil has no Spring dependencies — it just needs a secret string
 * and an expiry time. So we create a real JwtUtil instance directly
 * in the test, no Spring context needed (this makes tests very fast).
 *
 * TOOLS USED:
 * - JUnit 5 (@Test, @BeforeEach, @DisplayName)
 * - AssertJ (assertThat — fluent readable assertions)
 * - No Mockito needed here — everything is real
 */
class JwtUtilTest {

    // A long enough secret for HMAC-SHA signing (must be ≥ 256 bits / 32 chars)
    private static final String SECRET =
            "test-secret-key-min-256-bits-long-for-testing-only-x";

    private JwtUtil jwtUtil;

    /**
     * @BeforeEach runs before EVERY test method.
     * Creates a fresh JwtUtil with a 1-hour expiry.
     */
    @BeforeEach
    void setUp() {
        // 1-hour expiry for normal tests
        jwtUtil = new JwtUtil(SECRET, 1L);
    }

    // ─────────────────────────────────────────────────────────
    // Token Generation
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("generateToken returns a non-blank token string")
    void generateToken_returnsNonBlankToken() {
        String token = jwtUtil.generateToken("alice@test.com");

        // A JWT always has exactly two dots (header.payload.signature)
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    // ─────────────────────────────────────────────────────────
    // Email Extraction
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("extractEmail returns the email that was put into the token")
    void extractEmail_returnsCorrectEmail() {
        String email = "alice@test.com";
        String token = jwtUtil.generateToken(email);

        assertThat(jwtUtil.extractEmail(token)).isEqualTo(email);
    }

    // ─────────────────────────────────────────────────────────
    // Validity
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("isValid returns true for a freshly generated token")
    void isValid_trueForFreshToken() {
        String token = jwtUtil.generateToken("bob@test.com");

        assertThat(jwtUtil.isValid(token)).isTrue();
    }

    @Test
    @DisplayName("isValid returns false for a token that has already expired")
    void isValid_falseForExpiredToken() {
        // Create a JwtUtil with 0-hour expiry → token expires instantly
        JwtUtil expiredUtil = new JwtUtil(SECRET, 0L);
        String token = expiredUtil.generateToken("expired@test.com");

        // Token was created with 0 ms validity — should already be invalid
        assertThat(expiredUtil.isValid(token)).isFalse();
    }

    @Test
    @DisplayName("isValid returns false for a completely garbage string")
    void isValid_falseForGarbageToken() {
        assertThat(jwtUtil.isValid("this.is.garbage")).isFalse();
    }

    @Test
    @DisplayName("isValid returns false for a token signed with a different secret")
    void isValid_falseForTokenSignedWithWrongSecret() {
        // Create a token with a DIFFERENT secret key
        JwtUtil otherUtil = new JwtUtil(
                "completely-different-secret-key-min-256-bits-long!!", 1L);
        String foreignToken = otherUtil.generateToken("hacker@test.com");

        // Our jwtUtil should reject a token it didn't sign
        assertThat(jwtUtil.isValid(foreignToken)).isFalse();
    }

    @Test
    @DisplayName("isValid returns false for an empty string")
    void isValid_falseForEmptyString() {
        assertThat(jwtUtil.isValid("")).isFalse();
    }

    @Test
    @DisplayName("Different emails produce different tokens")
    void generateToken_differentEmailsProduceDifferentTokens() {
        String token1 = jwtUtil.generateToken("alice@test.com");
        String token2 = jwtUtil.generateToken("bob@test.com");

        assertThat(token1).isNotEqualTo(token2);
    }
}
