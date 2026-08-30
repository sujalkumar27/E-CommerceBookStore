package com.bookstore.model;

/**
 * ============================================================
 * OrderStatus — The Lifecycle States of an Order
 * ============================================================
 *
 * WHAT THIS IS:
 * An enum (a fixed set of named values) representing every possible
 * state an order can be in during its lifecycle.
 *
 * THE LIFECYCLE:
 *
 *   PENDING
 *     ↓  (payment confirmed)
 *   CONFIRMED  ←── cancellation allowed within 48 hours from here
 *     ↓  (shipped by warehouse)
 *   SHIPPED
 *     ↓  (delivered to customer)
 *   DELIVERED
 *
 *   CONFIRMED / SHIPPED / DELIVERED
 *     ↓  (customer cancels within 48-hr window)
 *   CANCELLED
 *
 * BUSINESS RULE (D-002):
 *   An order can be cancelled within 48 hours of payment confirmation
 *   regardless of which status it is in (CONFIRMED, SHIPPED, DELIVERED).
 *   After 48 hours, cancellation is not allowed.
 *
 * NOTE: For this capstone, payment is SIMULATED.
 *   Orders go directly to CONFIRMED after simulated payment success.
 *   SHIPPED and DELIVERED are included for completeness but are not
 *   triggered by any automated process in this version.
 */
public enum OrderStatus {

    /**
     * Order has been created but payment has not been confirmed yet.
     * In this capstone, orders skip PENDING and go straight to CONFIRMED
     * because payment is simulated synchronously.
     */
    PENDING,

    /**
     * Payment has been confirmed (simulated).
     * The 48-hour cancellation window starts from this moment.
     * payment_confirmed_at timestamp is set when entering this state.
     */
    CONFIRMED,

    /**
     * The order has been dispatched from the warehouse.
     * Cancellation is still allowed within the 48-hour window (D-002).
     */
    SHIPPED,

    /**
     * The order has been delivered to the customer.
     * Cancellation is still allowed within the 48-hour window (D-002).
     */
    DELIVERED,

    /**
     * The order has been cancelled by the customer.
     * Only possible within 48 hours of payment_confirmed_at.
     */
    CANCELLED
}
