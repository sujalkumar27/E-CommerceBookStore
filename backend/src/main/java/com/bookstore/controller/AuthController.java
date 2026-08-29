package com.bookstore.controller;

import com.bookstore.dto.auth.AuthResponse;
import com.bookstore.dto.auth.LoginRequest;
import com.bookstore.dto.auth.RegisterRequest;
import com.bookstore.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * ============================================================
 * AuthController — Handles Login and Registration HTTP Requests
 * ============================================================
 *
 * WHAT THIS IS:
 * The "front door" for authentication-related HTTP requests.
 * It receives requests from the frontend, validates the input,
 * calls AuthService to do the actual work, and returns the result.
 *
 * RULE: Controllers contain NO business logic.
 *       They only: receive → validate → delegate → respond.
 *
 * BASE URL: /api/auth
 *
 * ENDPOINTS:
 *   POST /api/auth/register  → Create a new account
 *   POST /api/auth/login     → Log in to an existing account
 *   POST /api/auth/logout    → Log out (client-side token discard)
 *
 * All endpoints in this controller are PUBLIC (no token required).
 * This is configured in SecurityConfig: .requestMatchers("/api/auth/**").permitAll()
 *
 * @RestController = @Controller + @ResponseBody
 *   Means: this class handles HTTP requests and returns JSON automatically
 * @RequestMapping("/api/auth") = all methods in this class start with /api/auth
 * @RequiredArgsConstructor = Lombok generates constructor for final fields
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    // The service that contains all the register/login logic
    private final AuthService authService;

    /**
     * POST /api/auth/register
     *
     * Creates a new user account.
     *
     * REQUEST BODY:
     * {
     *   "email": "alice@test.com",
     *   "password": "mypassword123"
     * }
     *
     * RESPONSE 201 Created:
     * {
     *   "token": "eyJhbGciOiJ...",
     *   "user": { "id": "...", "email": "...", "giftPointBalance": 0 }
     * }
     *
     * RESPONSE 400 Bad Request: if email or password is invalid/missing
     * RESPONSE 409 Conflict:    if email is already registered
     *
     * @Valid triggers the validation annotations in RegisterRequest
     * @RequestBody reads the JSON body from the HTTP request
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response); // 201 Created
    }

    /**
     * POST /api/auth/login
     *
     * Logs in an existing user and returns a JWT token.
     *
     * REQUEST BODY:
     * {
     *   "email": "alice@test.com",
     *   "password": "mypassword123"
     * }
     *
     * RESPONSE 200 OK:
     * {
     *   "token": "eyJhbGciOiJ...",
     *   "user": { "id": "...", "email": "..." }
     * }
     *
     * RESPONSE 401 Unauthorized: if email or password is wrong
     *   (always a generic message — never reveals which field is wrong)
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response); // 200 OK
    }

    /**
     * POST /api/auth/logout
     *
     * JWT tokens are stateless — the server doesn't store them.
     * "Logout" means the frontend simply deletes the token from localStorage.
     * This endpoint acknowledges the logout request but does nothing server-side.
     *
     * In a future enhancement, we could maintain a token denylist
     * to truly invalidate tokens before they expire.
     *
     * RESPONSE 200 OK: { "message": "Logged out successfully." }
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(Map.of("message", "Logged out successfully."));
    }
}
