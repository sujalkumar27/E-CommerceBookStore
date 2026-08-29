package com.bookstore.exception;

/**
 * BusinessRuleException
 *
 * Thrown when a business rule is violated.
 * Examples:
 *   - Trying to cancel an order after the 48-hour window
 *   - Trying to redeem more gift points than the user has
 *   - Trying to checkout with an empty basket
 *
 * The GlobalExceptionHandler catches this and returns HTTP 400 Bad Request.
 */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
