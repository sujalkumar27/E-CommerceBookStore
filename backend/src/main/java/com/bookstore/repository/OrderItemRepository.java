package com.bookstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * ============================================================
 * OrderItemRepository — Database Operations for Order Items
 * ============================================================
 *
 * WHAT THIS IS:
 * Handles queries for the "order_items" table.
 * The OrderItem entity itself is built in Phase 5.
 *
 * QUERIES USED BY RecommendationService:
 * These three queries look at a user's past orders to build
 * personalised recommendations (D-010):
 *
 *   findOrderedBookIdsByUser    → "what books has this user ordered?"
 *   findOrderedCategoryIdsByUser → "what categories has this user ordered from?"
 *   findOrderedAuthorsByUser    → "what authors has this user ordered?"
 *
 * NOTE: The full OrderItem entity and repository is completed in Phase 5.
 * This file is created in Phase 4 because RecommendationService needs it.
 */
@Repository
public interface OrderItemRepository extends JpaRepository<Object, UUID> {

    /**
     * Get all book IDs that a user has ever ordered.
     * Used to EXCLUDE already-ordered books from recommendations.
     *
     * @param userId - the user's UUID
     * @return list of book UUIDs the user has ordered
     */
    @Query(value = """
            SELECT oi.book_id FROM order_items oi
            JOIN orders o ON oi.order_id = o.id
            WHERE o.user_id = :userId
            """, nativeQuery = true)
    List<UUID> findOrderedBookIdsByUser(@Param("userId") UUID userId);

    /**
     * Get all category IDs from a user's past orders.
     * Used for Signal A: "recommend books in same categories".
     *
     * @param userId - the user's UUID
     * @return list of distinct category UUIDs from past orders
     */
    @Query(value = """
            SELECT DISTINCT b.category_id FROM order_items oi
            JOIN orders o ON oi.order_id = o.id
            JOIN books b ON oi.book_id = b.id
            WHERE o.user_id = :userId
            """, nativeQuery = true)
    List<UUID> findOrderedCategoryIdsByUser(@Param("userId") UUID userId);

    /**
     * Get all distinct author names from a user's past orders.
     * Used for Signal B: "recommend books by same authors".
     *
     * @param userId - the user's UUID
     * @return list of distinct author name strings from past orders
     */
    @Query(value = """
            SELECT DISTINCT b.author FROM order_items oi
            JOIN orders o ON oi.order_id = o.id
            JOIN books b ON oi.book_id = b.id
            WHERE o.user_id = :userId
            """, nativeQuery = true)
    List<String> findOrderedAuthorsByUser(@Param("userId") UUID userId);
}
