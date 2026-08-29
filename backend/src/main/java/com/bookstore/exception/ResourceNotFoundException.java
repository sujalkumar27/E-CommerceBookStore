package com.bookstore.exception;

/**
 * ResourceNotFoundException
 *
 * Thrown when a requested resource does not exist in the database.
 * Examples:
 *   - GET /api/books/abc-123  → book with that ID doesn't exist
 *   - GET /api/orders/xyz-456 → order with that ID doesn't exist
 *
 * The GlobalExceptionHandler catches this and returns HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
