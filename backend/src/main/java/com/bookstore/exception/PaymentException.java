package com.bookstore.exception;

/**
 * PaymentException
 *
 * Thrown when the simulated payment fails.
 * In this capstone project, payment is not real — it is simulated.
 * The simulation randomly fails ~10% of the time to mimic real-world behaviour.
 *
 * The GlobalExceptionHandler catches this and returns HTTP 402 Payment Required.
 */
public class PaymentException extends RuntimeException {
    public PaymentException(String message) {
        super(message);
    }
}
