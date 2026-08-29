package com.bookstore.repository;

import com.bookstore.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * ============================================================
 * UserRepository — Database Operations for Users
 * ============================================================
 *
 * WHAT THIS IS:
 * This interface is our "filing clerk" for the users table.
 * We never write SQL by hand — Spring Data JPA generates the
 * SQL queries automatically just from the method names.
 *
 * HOW IT WORKS:
 * By extending JpaRepository<User, UUID>, we automatically get:
 *   - save(user)           → INSERT or UPDATE
 *   - findById(id)         → SELECT WHERE id = ?
 *   - findAll()            → SELECT * FROM users
 *   - delete(user)         → DELETE
 *   - existsById(id)       → SELECT COUNT(*) > 0
 *   - count()              → SELECT COUNT(*)
 *   ...and many more
 *
 * The <User, UUID> means:
 *   - User  = the entity this repository manages
 *   - UUID  = the type of the primary key (id column)
 *
 * CUSTOM METHODS:
 * Spring generates SQL for custom methods based on their name:
 *   findByEmail("alice@test.com")
 *   → SELECT * FROM users WHERE email = 'alice@test.com'
 *
 *   existsByEmail("alice@test.com")
 *   → SELECT COUNT(*) > 0 FROM users WHERE email = 'alice@test.com'
 */
@Repository  // Marks this as a Spring-managed database component
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find a user by their email address.
     * Used during login to load the user and verify their password.
     *
     * Optional<User> means: returns a User if found, or empty if not found.
     * This is safer than returning null (avoids NullPointerExceptions).
     *
     * @param email - the email address to search for
     * @return Optional containing the User if found, empty otherwise
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if a user with this email already exists.
     * Used during registration to prevent duplicate accounts.
     *
     * @param email - the email to check
     * @return true if a user with this email exists, false otherwise
     */
    boolean existsByEmail(String email);
}
