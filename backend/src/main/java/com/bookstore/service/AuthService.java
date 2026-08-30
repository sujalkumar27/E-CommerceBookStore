package com.bookstore.service;

import com.bookstore.dto.auth.AuthResponse;
import com.bookstore.dto.auth.LoginRequest;
import com.bookstore.dto.auth.RegisterRequest;
import com.bookstore.exception.ConflictException;
import com.bookstore.model.User;
import com.bookstore.repository.UserRepository;
import com.bookstore.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * ============================================================
 * AuthService — Business Logic for Registration and Login
 * ============================================================
 *
 * WHAT THIS DOES:
 * Contains all the logic for creating accounts and logging in.
 * The controller just calls these methods — all decisions happen here.
 *
 * REGISTER FLOW:
 *   1. Check email is not already taken
 *   2. Hash the password with BCrypt
 *   3. Save the new user to the database
 *   4. Generate a JWT token
 *   5. Return the token + user info
 *
 * LOGIN FLOW:
 *   1. Ask Spring Security's AuthenticationManager to verify credentials
 *      (it loads the user from DB and compares the BCrypt hash)
 *   2. If wrong credentials → AuthenticationManager throws BadCredentialsException
 *      → GlobalExceptionHandler returns 401 with generic message
 *   3. If correct → load the user, generate JWT, return it
 *
 * @Service — tells Spring this is a service layer component
 * @RequiredArgsConstructor — Lombok generates constructor for all final fields
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;          // BCrypt encoder
    private final JwtUtil jwtUtil;                          // JWT token generator
    private final AuthenticationManager authenticationManager; // Verifies credentials

    /**
     * Register a new user account.
     *
     * @param request - contains email and plain-text password from the client
     * @return AuthResponse with JWT token and user details
     * @throws ConflictException if the email is already registered
     */
    public AuthResponse register(RegisterRequest request) {

        // Step 1: Check if this email is already taken
        // If it is, throw a ConflictException → 409 response
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("An account with this email already exists.");
        }

        // Step 2: Hash the password — NEVER store plain text
        // BCrypt produces something like "$2b$12$X9Kd3mN..."
        String hashedPassword = passwordEncoder.encode(request.password());

        // Step 3: Create and save the new User entity
        User user = new User(request.name(), request.email(), hashedPassword);
        userRepository.save(user);

        // Step 4: Generate a JWT token for the new user
        // They are logged in immediately after registering
        String token = jwtUtil.generateToken(user.getEmail());

        // Step 5: Build and return the response
        return buildResponse(token, user);
    }

    /**
     * Log in an existing user.
     *
     * @param request - contains email and plain-text password from the client
     * @return AuthResponse with JWT token and user details
     * @throws BadCredentialsException if email or password is wrong
     *         (caught by GlobalExceptionHandler → 401 with generic message)
     */
    public AuthResponse login(LoginRequest request) {

        // Step 1: Ask Spring Security to verify credentials
        // This loads the user from DB and compares the BCrypt hash
        // If wrong → throws BadCredentialsException automatically
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),    // username
                        request.password()  // plain-text password to check
                )
        );

        // Step 2: Credentials are correct — load the user from DB
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(); // Should never throw here (already authenticated above)

        // Step 3: Generate a JWT token
        String token = jwtUtil.generateToken(user.getEmail());

        // Step 4: Return the token + user details
        return buildResponse(token, user);
    }

    /**
     * Helper: builds the AuthResponse from a token and user.
     * Used by both register() and login() to avoid code duplication.
     */
    private AuthResponse buildResponse(String token, User user) {
        return new AuthResponse(
                token,
                new AuthResponse.UserInfo(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getGiftPointBalance(),
                        user.getCreatedAt()
                )
        );
    }
}
