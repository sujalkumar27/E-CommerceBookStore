package com.bookstore.repository;

import com.bookstore.model.BasketItem;
import com.bookstore.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ============================================================
 * BasketRepository — Database Operations for Basket Items
 * ============================================================
 *
 * WHAT THIS DOES:
 * All database queries for the "basket_items" table.
 *
 * KEY METHODS:
 *   findAllByUser()          → get all items in a user's basket
 *   findByUserAndBookId()    → check if a specific book is already in basket
 *   deleteAllByUser()        → clear the basket after checkout
 */
@Repository
public interface BasketRepository extends JpaRepository<BasketItem, UUID> {

    /**
     * Get all basket items for a specific user.
     * Used to display the basket contents page.
     * Ordered by addedAt so items appear in the order they were added.
     *
     * @param user - the logged-in user
     * @return list of all their basket items
     */
    List<BasketItem> findAllByUserOrderByAddedAtAsc(User user);

    /**
     * Check if a specific book is already in the user's basket.
     * Used when adding an item — if it already exists, we update quantity
     * instead of inserting a duplicate row.
     *
     * @param user   - the logged-in user
     * @param bookId - the book's UUID
     * @return Optional<BasketItem> — present if found, empty if not
     */
    Optional<BasketItem> findByUserAndBookId(User user, UUID bookId);

    /**
     * Delete ALL basket items for a user.
     * Called after a successful payment to clear the basket.
     *
     * @param user - the logged-in user whose basket to clear
     */
    void deleteAllByUser(User user);

    /**
     * Count how many items are in a user's basket.
     * Used to show the basket item count badge in the header.
     *
     * @param user - the logged-in user
     * @return number of distinct items (not total quantity)
     */
    int countByUser(User user);
}
