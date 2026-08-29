package com.bookstore.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * ============================================================
 * JwtUtil — Creates and Validates JWT Tokens
 * ============================================================
 *
 * WHAT IS A JWT TOKEN?
 * A JWT (JSON Web Token) is like a signed ID card given to a user after login.
 * It contains:
 *   - The user's email (so we know who they are)
 *   - An expiry time (so it stops working after 24 hours)
 *   - A digital signature (so nobody can fake or tamper with it)
 *
 * A token looks like this (three parts separated by dots):
 *   eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbGljZUBnbWFpbC5jb20i.XYZ...
 *   |_____ header _____|.|________ payload (email, expiry) __|.|sig|
 *
 * HOW IT WORKS:
 *   1. User logs in → we create a token with their email, sign it, return it
 *   2. User sends the token with every request
 *   3. We verify the signature to ensure it hasn't been tampered with
 *   4. We extract the email to know who is making the request
 *
 * SECURITY:
 *   - The signing key comes from the JWT_SECRET environment variable
 *   - Never hardcoded in source code
 */
@Component  // Tells Spring to create one instance of this class and share it everywhere
public class JwtUtil {

    // The secret key used to sign (and verify) tokens
    private final SecretKey signingKey;

    // How long (in milliseconds) a token stays valid
    private final long expiryMillis;

    /**
     * Constructor — reads config values from application.yml.
     *
     * @param secret      - the JWT secret string (from JWT_SECRET env var)
     * @param expiryHours - how many hours the token is valid (from JWT_EXPIRY_HOURS)
     */
    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiry-hours}") long expiryHours) {
        // Convert the secret string into a proper cryptographic key
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        // Convert hours to milliseconds (1 hour = 3,600,000 ms)
        this.expiryMillis = expiryHours * 3_600_000L;
    }

    /**
     * Generate a new JWT token for a user after they log in.
     *
     * @param email - the logged-in user's email address
     * @return a signed JWT string to send back to the client
     */
    public String generateToken(String email) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(email)                         // Store the user's email in the token
                .issuedAt(new Date(now))                // When the token was created
                .expiration(new Date(now + expiryMillis)) // When the token expires
                .signWith(signingKey)                   // Sign it with our secret key
                .compact();                             // Build the final token string
    }

    /**
     * Extract the user's email from a token.
     * Called after we've already verified the token is valid.
     *
     * @param token - the JWT string from the request header
     * @return the email stored inside the token
     */
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Check if a token is valid (correct signature + not expired).
     *
     * @param token - the JWT string to check
     * @return true if valid, false if expired or tampered with
     */
    public boolean isValid(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration().after(new Date()); // Not expired?
        } catch (JwtException | IllegalArgumentException e) {
            // Token is malformed, tampered with, or expired → not valid
            return false;
        }
    }

    /**
     * Parse the token and return its claims (the data stored inside).
     * This also verifies the signature — throws an exception if invalid.
     *
     * @param token - the JWT string
     * @return Claims object containing subject, expiry, etc.
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)  // Use our secret key to verify the signature
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
