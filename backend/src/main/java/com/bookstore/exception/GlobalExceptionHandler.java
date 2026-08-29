package com.bookstore.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================
 * GlobalExceptionHandler — Catches ALL Errors in One Place
 * ============================================================
 *
 * PROBLEM THIS SOLVES:
 * Without this, if something goes wrong (e.g. book not found),
 * Spring would return a messy default error with stack traces
 * that could expose internal system details to users — a security risk.
 *
 * WHAT THIS DOES:
 * Intercepts every exception thrown anywhere in the application
 * and returns a clean, consistent JSON error response like:
 * {
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Book not found",
 *   "timestamp": "2026-08-29T10:00:00Z",
 *   "path": "/api/books/abc-123"
 * }
 *
 * SECURITY RULE:
 * - Return GENERIC messages to the client (no stack traces, no system info)
 * - Log FULL details server-side only (so we can debug without exposing info)
 *
 * @RestControllerAdvice = applies to all @RestController classes in the app
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Logger — writes details to server log (not returned to client)
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Handles: trying to access a resource that doesn't exist (e.g. book ID not found) */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req.getRequestURI());
    }

    /** Handles: trying to access something belonging to another user */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), req.getRequestURI());
    }

    /** Handles: violating a business rule (e.g. cancelling after 48 hours) */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req.getRequestURI());
    }

    /** Handles: conflict (e.g. email already registered, order already cancelled) */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req.getRequestURI());
    }

    /** Handles: simulated payment failure */
    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ErrorResponse> handlePayment(PaymentException ex, HttpServletRequest req) {
        return build(HttpStatus.PAYMENT_REQUIRED, ex.getMessage(), req.getRequestURI());
    }

    /**
     * Handles: @Valid validation failures (e.g. missing required fields in request body).
     * Returns field-level error details so the frontend can highlight specific fields.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        // Collect all field errors into a map: { "email": "must not be blank" }
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        ErrorResponse body = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "Validation failed",
                Instant.now().toString(),
                req.getRequestURI(),
                fieldErrors  // Include field-level errors in the response
        );
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Handles: wrong email or password during login.
     * IMPORTANT: we return a GENERIC message — never say "email not found"
     * or "wrong password" as that helps attackers know which field is wrong.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "Invalid credentials.", req.getRequestURI());
    }

    /** Handles: valid token but trying to access someone else's resource */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "Access denied.", req.getRequestURI());
    }

    /**
     * Catch-all: handles any unexpected exception.
     * Full details are logged server-side; only a generic message is returned to the client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, HttpServletRequest req) {
        // Log the full error server-side (for debugging) — never send to client
        log.error("Unhandled exception at {}: {}", req.getRequestURI(), ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", req.getRequestURI());
    }

    /** Helper: builds the standard error response object */
    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, String path) {
        ErrorResponse body = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                Instant.now().toString(),
                path,
                null  // No field errors for non-validation errors
        );
        return ResponseEntity.status(status).body(body);
    }

    /**
     * ErrorResponse — the standard JSON error shape returned to clients.
     *
     * Example:
     * {
     *   "status": 404,
     *   "error": "Not Found",
     *   "message": "Book not found",
     *   "timestamp": "2026-08-29T10:00:00Z",
     *   "path": "/api/books/abc-123",
     *   "fieldErrors": null
     * }
     *
     * Java "record" = an immutable data class (auto-generates getters, equals, toString)
     */
    public record ErrorResponse(
            int status,
            String error,
            String message,
            String timestamp,
            String path,
            Map<String, String> fieldErrors
    ) {}
}
