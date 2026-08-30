package com.bookstore.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * ============================================================
 * OrderItem — One Book Line in an Order
 * ============================================================
 *
 * WHAT THIS IS:
 * Maps to the "order_items" table.
 * Each OrderItem is one book within an order.
 * If a customer orders 3 different books, there are 3 OrderItem rows.
 *
 * KEY FIELDS EXPLAINED:
 *
 *   unitPrice (PRICE SNAPSHOT):
 *     We store the price AT THE TIME of ordering, not the current price.
 *     If the book price changes tomorrow, this order still shows what
 *     the customer actually paid. This is critical for financial accuracy.
 *
 *   tentativeDeliveryDate:
 *     Calculated at order creation time:
 *     = order date + category.deliveryOffsetDays (D-005)
 *     Example: Fiction book ordered on Aug 30 → delivery Sep 2 (3 days)
 *     Stored as a date (not datetime) — just the day.
 *
 * DATABASE TABLE: order_items
 * ┌────────────────────────┬────────────────────────────────────┐
 * │ Column                 │ Description                        │
 * ├────────────────────────┼────────────────────────────────────┤
 * │ id                     │ UUID primary key                   │
 * │ order_id               │ FK → orders.id                    │
 * │ book_id                │ FK → books.id                     │
 * │ quantity               │ How many copies ordered            │
 * │ unit_price             │ Price at time of order (snapshot)  │
 * │ tentative_delivery_date│ Expected delivery date             │
 * └────────────────────────┴────────────────────────────────────┘
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * The order this item belongs to.
     * @ManyToOne = many items belong to one order.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * The book that was ordered.
     * We keep the reference so we can show book details in order history.
     * ON DELETE RESTRICT — book cannot be deleted if it has order history.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    /** How many copies were ordered */
    @Column(nullable = false)
    private int quantity;

    /**
     * The book's price at the time this order was placed.
     * NEVER updated after creation — historical accuracy.
     */
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    /**
     * Expected delivery date for this item.
     * Calculated at order creation: order date + category.deliveryOffsetDays
     * Example: Technology book → +5 days from order date
     */
    @Column(name = "tentative_delivery_date", nullable = false)
    private LocalDate tentativeDeliveryDate;

    /**
     * Convenience constructor used in PaymentService.
     */
    public OrderItem(Order order, Book book, int quantity, BigDecimal unitPrice, LocalDate tentativeDeliveryDate) {
        this.order = order;
        this.book = book;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.tentativeDeliveryDate = tentativeDeliveryDate;
    }

    /** lineTotal = unitPrice × quantity */
    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
