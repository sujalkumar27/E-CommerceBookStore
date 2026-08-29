package com.bookstore.repository;

import com.bookstore.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * ============================================================
 * CategoryRepository — Database Operations for Categories
 * ============================================================
 *
 * WHAT THIS DOES:
 * Handles all database queries for the "categories" table.
 * Used by SeedDataLoader to create categories if they don't exist,
 * and by BookController/BookService to return category lists.
 *
 * INHERITED METHODS (from JpaRepository):
 *   findAll()        → SELECT * FROM categories
 *   findById(id)     → SELECT * WHERE id = ?
 *   save(category)   → INSERT or UPDATE
 *   existsById(id)   → SELECT COUNT(*) > 0
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    /**
     * Find a category by its name (case-sensitive).
     * Used by SeedDataLoader to check if a category already exists before inserting.
     *
     * Example: findByName("Fiction") → Optional containing the Fiction category row
     *
     * @param name - the category name to search for
     * @return Optional<Category> — present if found, empty if not
     */
    Optional<Category> findByName(String name);
}
