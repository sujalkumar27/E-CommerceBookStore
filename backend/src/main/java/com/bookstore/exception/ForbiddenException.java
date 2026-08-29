package com.bookstore.exception;

/**
 * ForbiddenException
 *
 * Thrown when a user tries to access a resource that belongs to someone else.
 * Examples:
 *   - User A tries to cancel User B's order
 *   - User A tries to delete User B's address
 *
 * The GlobalExceptionHandler catches this and returns HTTP 403 Forbidden.
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
