package com.bookstore.exception;

/**
 * ConflictException
 *
 * Thrown when a request conflicts with the current state of the data.
 * Examples:
 *   - Trying to register with an email that already exists
 *   - Trying to cancel an order that is already cancelled
 *
 * The GlobalExceptionHandler catches this and returns HTTP 409 Conflict.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
