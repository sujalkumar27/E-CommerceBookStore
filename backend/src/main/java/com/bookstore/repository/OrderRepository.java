package com.bookstore.repository;

import com.bookstore.model.Order;
import com.bookstore.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * ============================================================
 * OrderRepository — Database Operations for Orders
 * ============================================================
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Get all orders for a user, newest first.
     * Used for order history page (FS-006).
     *
     * @param user - the logged-in user
     * @return list of orders newest first
     */
    List<Order> findAllByUserOrderByCreatedAtDesc(User user);
}
