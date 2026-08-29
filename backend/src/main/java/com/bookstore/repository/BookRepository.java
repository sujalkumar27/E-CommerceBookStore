package com.bookstore.repository;

import com.bookstore.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * ============================================================
 * BookRepository — Database Operations for Books
 * ============================================================
 *
 * WHAT THIS DOES:
 * All database queries for the "books" table.
 * This includes: search, filter, find by ID, find related books,
 * find by author (for recommendations).
 *
 * KEY CONCEPTS:
 *
 * Page<Book> and Pageable:
 *   Instead of returning ALL books (could be thousands), we return
 *   one "page" at a time. The caller says "give me page 0, 20 books per page".
 *   Spring handles the SQL LIMIT and OFFSET automatically.
 *
 * @Query with JPQL:
 *   JPQL (Java Persistence Query Language) looks like SQL but uses
 *   Java class/field names, not table/column names.
 *   Example: "FROM Book b" instead of "FROM books b"
 *
 * :param notation:
 *   The :categoryId syntax is a "named parameter" — Spring replaces it
 *   with the actual value, preventing SQL injection attacks.
 */
@Repository
public interface BookRepository extends JpaRepository<Book, UUID> {

    /**
     * Main catalogue query — supports search + all filters combined.
     *
     * HOW THE QUERY WORKS:
     * - :search      → matches title, author, publisher, or category name
     *                  LOWER() makes it case-insensitive
     *                  LIKE '%keyword%' matches if keyword appears anywhere
     *                  The 'true = true' trick means: if search is null/empty,
     *                  skip the search condition entirely
     * - :categoryId  → filter by category UUID (null = no filter)
     * - :publisher   → filter by publisher name (null = no filter)
     * - :minPrice    → minimum price (null = no minimum)
     * - :maxPrice    → maximum price (null = no maximum)
     * - :available   → true = only in-stock books (null = all books)
     *
     * @param search     - free text search term (nullable)
     * @param categoryId - filter by category UUID (nullable)
     * @param publisher  - filter by publisher name (nullable)
     * @param minPrice   - minimum price filter (nullable)
     * @param maxPrice   - maximum price filter (nullable)
     * @param available  - true = in-stock only (nullable)
     * @param pageable   - page number and size
     * @return Page of books matching all provided filters
     */
    @Query("""
            SELECT b FROM Book b
            JOIN b.category c
            WHERE (:search IS NULL OR :search = '' OR (
                LOWER(b.title)     LIKE LOWER(CONCAT('%', :search, '%')) OR
                LOWER(b.author)    LIKE LOWER(CONCAT('%', :search, '%')) OR
                LOWER(b.publisher) LIKE LOWER(CONCAT('%', :search, '%')) OR
                LOWER(c.name)      LIKE LOWER(CONCAT('%', :search, '%'))
            ))
            AND (:categoryId IS NULL OR c.id = :categoryId)
            AND (:publisher  IS NULL OR LOWER(b.publisher) LIKE LOWER(CONCAT('%', :publisher, '%')))
            AND (:minPrice   IS NULL OR b.price >= :minPrice)
            AND (:maxPrice   IS NULL OR b.price <= :maxPrice)
            AND (:available  IS NULL OR (:available = TRUE AND b.stock > 0) OR :available = FALSE)
            ORDER BY b.createdAt DESC
            """)
    Page<Book> findWithFilters(
            @Param("search")     String search,
            @Param("categoryId") UUID categoryId,
            @Param("publisher")  String publisher,
            @Param("minPrice")   BigDecimal minPrice,
            @Param("maxPrice")   BigDecimal maxPrice,
            @Param("available")  Boolean available,
            Pageable pageable
    );

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
