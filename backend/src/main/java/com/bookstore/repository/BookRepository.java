package com.bookstore.repository;

import com.bookstore.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * ============================================================
 * BookRepository — Database Operations for Books
 * ============================================================
 *
 * WHAT THIS DOES:
 * All database queries for the "books" table.
 *
 * WHY WE REMOVED findWithFilters() JPQL QUERY:
 * Hibernate 6 + PostgreSQL has a known type-inference bug when you write
 *   ":categoryId IS NULL"  in JPQL where :categoryId is a UUID parameter.
 * Hibernate cannot figure out the SQL type of a NULL UUID value and throws
 * a ClassCastException or "could not determine data type" error at runtime.
 *
 * THE FIX — JpaSpecificationExecutor:
 * Instead of one big JPQL string, we build the WHERE clause programmatically
 * in BookSpecification.java.  Each filter is added only when its value is
 * non-null — so the "IS NULL" check is done in Java, not in SQL, and
 * Hibernate never sees a NULL UUID parameter.
 *
 * KEY CONCEPTS:
 *
 * JpaSpecificationExecutor<Book>:
 *   An extra interface from Spring Data JPA that adds a `findAll(Specification, Pageable)`
 *   method.  The Specification is a lambda that builds JPA Criteria predicates.
 *
 * Page<Book> and Pageable:
 *   Instead of returning ALL books (could be thousands), we return one
 *   "page" at a time.  Spring handles the SQL LIMIT and OFFSET automatically.
 */
@Repository
public interface BookRepository extends JpaRepository<Book, UUID>, JpaSpecificationExecutor<Book> {

    /**
     * Find related books — same category, excluding the current book.
     * Used on the product detail page (FS-003, D-006).
     *
     * Returns up to `limit` books from the same category.
     * Ordered by newest first so we show fresh stock.
     *
     * @param categoryId - the category to match
     * @param excludeId  - the current book's ID (don't recommend the same book)
     * @param pageable   - used to limit result count (pass PageRequest.of(0, 6))
     * @return list of related books (up to 6)
     */
    @Query("""
            SELECT b FROM Book b
            WHERE b.category.id = :categoryId
            AND b.id <> :excludeId
            ORDER BY b.createdAt DESC
            """)
    List<Book> findRelatedBooks(
            @Param("categoryId") UUID categoryId,
            @Param("excludeId")  UUID excludeId,
            Pageable pageable
    );

    /**
     * Find books by category IDs — used by the recommendation engine (D-010).
     * "Because you ordered Fiction" → find more Fiction books.
     *
     * @param categoryIds  - list of category UUIDs from past orders
     * @param excludeIds   - books the user already ordered (don't recommend again)
     * @param pageable     - limit result count
     * @return list of recommended books
     */
    @Query("""
            SELECT b FROM Book b
            WHERE b.category.id IN :categoryIds
            AND b.id NOT IN :excludeIds
            ORDER BY b.createdAt DESC
            """)
    List<Book> findByCategoryIdsExcluding(
            @Param("categoryIds") List<UUID> categoryIds,
            @Param("excludeIds")  List<UUID> excludeIds,
            Pageable pageable
    );

    /**
     * Find books by author names — used by the recommendation engine (D-010).
     * "Because you read J.K. Rowling" → find more J.K. Rowling books.
     *
     * LIKE matching is used because the author field may contain
     * multiple names (e.g. "J.K. Rowling, Mary GrandPré").
     *
     * @param authors    - list of author names from past orders
     * @param excludeIds - books the user already ordered
     * @param pageable   - limit result count
     * @return list of recommended books
     */
    @Query("""
            SELECT b FROM Book b
            WHERE (:#{#authors.size()} = 0 OR EXISTS (
                SELECT 1 FROM Book b2
                WHERE b2.id = b.id
                AND (
                    LOWER(b2.author) LIKE LOWER(CONCAT('%', :#{#authors[0]}, '%'))
                )
            ))
            AND b.id NOT IN :excludeIds
            ORDER BY b.createdAt DESC
            """)
    List<Book> findByAuthorsExcluding(
            @Param("authors")    List<String> authors,
            @Param("excludeIds") List<UUID> excludeIds,
            Pageable pageable
    );

    /**
     * Find the newest books added to the catalogue.
     * Used as the fallback recommendation signal (D-010 — newest additions).
     *
     * @param excludeIds - books already ordered by the user
     * @param pageable   - limit result count
     * @return newest books not yet ordered by the user
     */
    @Query("""
            SELECT b FROM Book b
            WHERE b.id NOT IN :excludeIds
            ORDER BY b.createdAt DESC
            """)
    List<Book> findNewestExcluding(
            @Param("excludeIds") List<UUID> excludeIds,
            Pageable pageable
    );

    /**
     * Check if a book with this ISBN already exists in the database.
     * Used by SeedDataLoader to skip duplicate books on restart.
     *
     * @param isbn - the ISBN to check
     * @return true if the book exists, false otherwise
     */
    boolean existsByIsbn(String isbn);
}
