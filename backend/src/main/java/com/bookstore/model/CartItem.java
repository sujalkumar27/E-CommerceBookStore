package com.bookstore.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * ============================================================
 * CartItem — One Book in a User's Shopping Cart
 * ============================================================
 *
 * WHAT THIS IS:
 * Maps to the "cart_items" table in the database.
 * Each row = one book that a logged-in user has added to their cart.
 *
 * RELATIONSHIP:
 *   One User  → many CartItems  (a user can have many books in cart)
 *   One Book  → many CartItems  (same book can be in many users' carts)
 *
 * KEY DESIGN DECISIONS:
 * - Cart is stored SERVER-SIDE (in the database), not in browser memory.
 *   This means if the user closes the browser and comes back, their cart
 *   is still there.
 * - UNIQUE constraint on (user_id, book_id) means a user cannot have the
 *   same book twice as separate rows — instead we update the quantity.
 *
 * DATABASE TABLE: cart_items
 * ┌──────────────┬─────────────────────────────────────────────┐
 * │ Column       │ Description                                 │
 * ├──────────────┼─────────────────────────────────────────────┤
 * │ id           │ UUID primary key                            │
 * │ user_id      │ FK → users.id (whose cart is this?)        │
 * │ book_id      │ FK → books.id (which book?)                 │
 * │ quantity     │ How many copies (minimum 1)                 │
 * │ added_at     │ When this item was added                    │
 * └──────────────┴─────────────────────────────────────────────┘
 */
@Entity
@Table(
    name = "cart_items",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "book_id"})
    // Same user cannot have same book as two separate rows.
    // If the book is already in the cart, we UPDATE the quantity instead.
)
@Getter
@Setter
@NoArgsConstructor
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * The user who owns this cart item.
     * @ManyToOne = many cart items belong to one user.
     * LAZY loading = only load the user from DB when we actually need them.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Which book this cart item represents.
     * @ManyToOne = many cart items can reference the same book.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    /**
     * How many copies of this book the user wants.
     * Minimum 1 — enforced by validation in the service.
     */
    @Column(nullable = false)
    private int quantity;

    /**
     * When this item was added to the cart.
     * Used to display items in the order they were added.
     */
    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt = Instant.now();

    /**
     * Convenience constructor used in CartService when adding a new item.
     */
    public CartItem(User user, Book book, int quantity) {
        this.user = user;
        this.book = book;
        this.quantity = quantity;
    }
}
