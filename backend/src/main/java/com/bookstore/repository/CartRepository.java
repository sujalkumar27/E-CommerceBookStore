package com.bookstore.repository;

import com.bookstore.model.CartItem;
import com.bookstore.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ============================================================
 * CartRepository — Database Operations for Cart Items
 * ============================================================
 *
 * WHAT THIS DOES:
 * All database queries for the "cart_items" table.
 *
 * KEY METHODS:
 *   findAllByUser()          → get all items in a user's cart
 *   findByUserAndBookId()    → check if a specific book is already in cart
 *   deleteAllByUser()        → clear the cart after checkout
 */
@Repository
public interface CartRepository extends JpaRepository<CartItem, UUID> {

    /**
     * Get all cart items for a specific user.
     * Used to display the cart contents page.
     * Ordered by addedAt so items appear in the order they were added.
     */
    List<CartItem> findAllByUserOrderByAddedAtAsc(User user);

    /**
     * Check if a specific book is already in the user's cart.
     * Used when adding an item — if it exists, update quantity instead.
     */
    Optional<CartItem> findByUserAndBookId(User user, UUID bookId);

    /**
     * Delete ALL cart items for a user.
     * Called after a successful payment to clear the cart.
     */
    void deleteAllByUser(User user);

    /**
     * Count how many items are in a user's cart.
     * Used for the cart item count badge in the header.
     */
    int countByUser(User user);
}
