package com.bookstore.repository;

import com.bookstore.model.Book;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ============================================================
 * BookSpecification — Type-Safe Book Filter Builder
 * ============================================================
 *
 * WHAT THIS IS:
 * A factory class that builds a JPA "Specification" object — a programmatic
 * way to construct a SQL WHERE clause without writing raw SQL or JPQL strings.
 *
 * WHY WE USE THIS (instead of a JPQL @Query):
 * Hibernate 6 + PostgreSQL has a type-inference bug: when you pass NULL for a
 * UUID or Boolean parameter in JPQL, Hibernate cannot figure out the SQL type
 * and throws an error.  In a Specification, each predicate (condition) is only
 * ADDED to the query when the value is non-null.  Hibernate never sees a null
 * UUID — so the bug never triggers.
 *
 * HOW A SPECIFICATION WORKS:
 *   1. Spring calls our lambda with (root, query, cb):
 *        root  = the Book entity alias (like "b" in "FROM Book b")
 *        query = the full query object (rarely needed here)
 *        cb    = CriteriaBuilder — a factory for building predicates (conditions)
 *
 *   2. We collect all active predicates (conditions) into a list.
 *      Only non-null parameters create predicates.
 *
 *   3. We combine them with cb.and(...) — every active condition must be true.
 *
 *   4. Spring generates and executes the final SQL automatically.
 *
 * EXAMPLE:
 *   getBooks(search="java", categoryId=null, minPrice=100, ...)
 *   → SQL WHERE (title LIKE '%java%' OR author LIKE '%java%' OR ...)
 *              AND price >= 100
 *   (no category condition because categoryId was null)
 */
public class BookSpecification {

    /**
     * Build a Specification that filters books by any combination of:
     * - free-text search (title / author / publisher / category name)
     * - category UUID
     * - publisher name
     * - price range (min / max)
     * - availability (in-stock only)
     *
     * Any null parameter is simply ignored (no condition added).
     *
     * @param search     - free text (nullable)
     * @param categoryId - UUID of the category to filter by (nullable)
     * @param publisher  - publisher name substring (nullable)
     * @param minPrice   - minimum price inclusive (nullable)
     * @param maxPrice   - maximum price inclusive (nullable)
     * @param available  - true = in-stock only (nullable = no filter)
     * @return a Specification ready to pass to bookRepository.findAll(spec, pageable)
     */
    public static Specification<Book> withFilters(
            String search,
            UUID categoryId,
            String publisher,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean available) {

        // The Specification is a lambda — Spring calls it with JPA criteria objects
        return (root, query, cb) -> {

            // We collect every active condition into this list
            List<Predicate> predicates = new ArrayList<>();

            // ── 1. FREE-TEXT SEARCH ───────────────────────────────────────────
            // Matches if the search term appears anywhere in:
            //   title, author, publisher, or category name.
            // LOWER() on both sides makes the match case-insensitive.
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";

                // Join to the category table so we can search on category name.
                // IMPORTANT: we use LEFT JOIN so books without a category are
                // still included (though all books have categories in our data).
                Join<Object, Object> category = root.join("category", JoinType.LEFT);

                Predicate titleMatch     = cb.like(cb.lower(root.get("title")),     pattern);
                Predicate authorMatch    = cb.like(cb.lower(root.get("author")),    pattern);
                Predicate publisherMatch = cb.like(cb.lower(root.get("publisher")), pattern);
                Predicate categoryMatch  = cb.like(cb.lower(category.get("name")),  pattern);

                // Book matches if ANY of the four fields contain the search term
                predicates.add(cb.or(titleMatch, authorMatch, publisherMatch, categoryMatch));
            }

            // ── 2. CATEGORY FILTER ────────────────────────────────────────────
            // Only adds this condition if categoryId is non-null.
            // No null UUID ever reaches SQL — the Hibernate 6 bug is avoided.
            if (categoryId != null) {
                // root.get("category").get("id") navigates the relationship:
                //   Book → category (FK join) → id column
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            // ── 3. PUBLISHER FILTER ───────────────────────────────────────────
            // Substring match on publisher name, case-insensitive.
            if (publisher != null && !publisher.isBlank()) {
                String pattern = "%" + publisher.toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("publisher")), pattern));
            }

            // ── 4. MINIMUM PRICE ──────────────────────────────────────────────
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }

            // ── 5. MAXIMUM PRICE ──────────────────────────────────────────────
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            // ── 6. AVAILABILITY FILTER ────────────────────────────────────────
            // available = true  → stock > 0  (only in-stock books)
            // available = false → no extra condition (show everything)
            // available = null  → no extra condition (show everything)
            if (Boolean.TRUE.equals(available)) {
                predicates.add(cb.greaterThan(root.get("stock"), 0));
            }

            // ── COMBINE ALL CONDITIONS WITH AND ───────────────────────────────
            // All active predicates must be true simultaneously.
            // If predicates is empty (no filters), this returns a WHERE clause
            // that is always true — i.e., all books are returned.
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
