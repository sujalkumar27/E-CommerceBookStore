package com.bookstore.service;

import com.bookstore.dto.auth.AuthResponse;
import com.bookstore.dto.auth.LoginRequest;
import com.bookstore.dto.auth.RegisterRequest;
import com.bookstore.exception.ConflictException;
import com.bookstore.model.User;
import com.bookstore.repository.UserRepository;
import com.bookstore.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ============================================================
 * AuthServiceTest — Unit Tests for Registration and Login
 * ============================================================
 *
 * WHAT WE ARE TESTING:
 * AuthService.register() and AuthService.login()
 *
 * KEY SCENARIOS:
 *   register:
 *     - Happy path: new email → user saved, token returned
 *     - Duplicate email → ConflictException thrown
 *
 *   login:
 *     - Happy path: correct credentials → token returned
 *     - Wrong credentials → BadCredentialsException propagated
 *
 * HOW MOCKITO WORKS HERE:
 * @Mock          creates a fake version of each dependency.
 *               The fake does nothing by default.
 * @InjectMocks   creates a real AuthService and injects all the @Mocks into it.
 * when(...).thenReturn(...)  programs what the fake should return.
 * verify(...)   checks that a method was actually called.
 *
 * @ExtendWith(MockitoExtension.class) activates Mockito for this test class.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    // ── Mocked dependencies (fakes — no real DB, no real BCrypt) ──
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthenticationManager authenticationManager;

    // ── The real class under test, with mocks injected ──
    @InjectMocks
    private AuthService authService;

    // ── Test data ──
    private static final String EMAIL    = "alice@test.com";
    private static final String PASSWORD = "password123";
    private static final String HASH     = "$2b$12$fakehash";
    private static final String TOKEN    = "eyJhbGci.faketoken.xyz";

    // A pre-built User we return from the fake repository
    private User savedUser;

    @BeforeEach
    void setUp() {
        savedUser = new User("Test User", EMAIL, HASH);
        // Assign a UUID so AuthResponse.UserInfo.id is populated
        // (Normally JPA assigns this on save — we set it manually in tests)
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(savedUser, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ─────────────────────────────────────────────────────────
    // register()
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("register: happy path — saves user and returns token")
    void register_happyPath_savesUserAndReturnsToken() {
        // Arrange: email not already taken
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.generateToken(EMAIL)).thenReturn(TOKEN);

        // Act
        RegisterRequest request = new RegisterRequest("Test User", EMAIL, PASSWORD);
        AuthResponse response = authService.register(request);

        // Assert: token and email are correct
        assertThat(response.token()).isEqualTo(TOKEN);
        assertThat(response.user().email()).isEqualTo(EMAIL);

        // Verify: password was hashed, user was saved, token was generated
        verify(passwordEncoder).encode(PASSWORD);
        verify(userRepository).save(any(User.class));
        verify(jwtUtil).generateToken(EMAIL);
    }

    @Test
    @DisplayName("register: throws ConflictException when email is already taken")
    void register_duplicateEmail_throwsConflictException() {
        // Arrange: this email is ALREADY in the database
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        RegisterRequest request = new RegisterRequest("Test User", EMAIL, PASSWORD);

        // Assert: ConflictException is thrown before any save happens
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");

        // Verify: save was NEVER called (we rejected before even hashing)
        verify(userRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────
    // login()
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("login: happy path — returns token for correct credentials")
    void login_happyPath_returnsToken() {
        // Arrange: AuthenticationManager accepts credentials (does not throw).
        // authenticate() returns an Authentication object (non-void) → use thenReturn, not doNothing.
        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(savedUser));
        when(jwtUtil.generateToken(EMAIL)).thenReturn(TOKEN);

        // Act
        LoginRequest request = new LoginRequest(EMAIL, PASSWORD);
        AuthResponse response = authService.login(request);

        // Assert
        assertThat(response.token()).isEqualTo(TOKEN);
        assertThat(response.user().email()).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("login: propagates BadCredentialsException for wrong password")
    void login_wrongCredentials_throwsBadCredentials() {
        // Arrange: AuthenticationManager rejects the credentials
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        LoginRequest request = new LoginRequest(EMAIL, "wrong-password");

        // Assert: the exception bubbles up (GlobalExceptionHandler converts it to 401)
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        // Verify: we never even looked up the user — failed at auth step
        verify(userRepository, never()).findByEmail(anyString());
    }
}

