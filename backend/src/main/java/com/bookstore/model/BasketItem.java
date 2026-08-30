package com.bookstore.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * ============================================================
 * BasketItem — One Book in a User's Shopping Basket
 * ============================================================
 *
 * WHAT THIS IS:
 * Maps to the "basket_items" table.
 * Each row = one book that a logged-in user has added to their basket.
 *
 * RELATIONSHIP:
 *   One User  → many BasketItems  (a user can have many books in basket)
 *   One Book  → many BasketItems  (same book can be in many users' baskets)
 *
 * KEY DESIGN DECISIONS:
 * - Basket is stored SERVER-SIDE (in the database), not in browser memory.
 *   This means if the user closes the browser and comes back, their basket
 *   is still there.
 * - UNIQUE constraint on (user_id, book_id) means a user cannot have the
 *   same book twice as separate rows — instead we update the quantity.
 *
 * DATABASE TABLE: basket_items
 * ┌──────────────┬─────────────────────────────────────────────┐
 * │ Column       │ Description                                 │
 * ├──────────────┼─────────────────────────────────────────────┤
 * │ id           │ UUID primary key                            │
 * │ user_id      │ FK → users.id (whose basket is this?)       │
 * │ book_id      │ FK → books.id (which book?)                 │
 * │ quantity     │ How many copies (minimum 1)                 │
 * │ added_at     │ When this item was added                    │
 * └──────────────┴─────────────────────────────────────────────┘
 */
@Entity
@Table(
    name = "basket_items",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "book_id"})
    // uniqueConstraint = same user cannot have same book as two separate rows
    // Instead, we UPDATE the quantity of the existing row
)
@Getter
@Setter
@NoArgsConstructor
public class BasketItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * The user who owns this basket item.
     * @ManyToOne = many basket items belong to one user.
     * LAZY loading = only load the user from DB when we actually need them.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Which book this basket item represents.
     * @ManyToOne = many basket items can reference the same book.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    /**
     * How many copies of this book the user wants.
     * Minimum 1 — enforced by @Column check and validation in the service.
     */
    @Column(nullable = false)
    private int quantity;

    /**
     * When this item was added to the basket.
     * Used to display items in the order they were added.
     */
    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt = Instant.now();

    /**
     * Convenience constructor used in BasketService.
     */
    public BasketItem(User user, Book book, int quantity) {
        this.user = user;
        this.book = book;
        this.quantity = quantity;
    }
}
