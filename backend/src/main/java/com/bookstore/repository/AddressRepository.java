package com.bookstore.repository;

import com.bookstore.model.Address;
import com.bookstore.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * ============================================================
 * AddressRepository — Database Operations for Addresses
 * ============================================================
 */
@Repository
public interface AddressRepository extends JpaRepository<Address, UUID> {

    /**
     * Get all saved addresses for a user.
     * Ordered by createdAt so newest appears last.
     */
    List<Address> findAllByUserOrderByCreatedAtAsc(User user);

    /**
     * Count how many addresses a user has saved.
     * Used to decide if we should auto-set is_default on the first address.
     */
    int countByUser(User user);
}
