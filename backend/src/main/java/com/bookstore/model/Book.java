package com.bookstore.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * ============================================================
 * Book — A Single Book in the Catalogue
 * ============================================================
 *
 * WHAT THIS IS:
 * Maps to the "books" table in the database.
 * Every book in the catalogue is one row in this table.
 * Books are loaded from seed.json at startup by SeedDataLoader.
 *
 * RELATIONSHIP TO CATEGORY:
 * Each book belongs to ONE category (Many books → One category).
 * In database terms: books.category_id references categories.id
 * In Java terms: @ManyToOne — many Books can have one Category.
 *
 * HOW SEARCH WORKS:
 * The database has a full-text search index on title, author, publisher.
 * We use Spring Data JPA's @Query to run search queries.
 *
 * DATABASE TABLE: books
 * ┌──────────────────┬────────────────────────────────────────────┐
 * │ Column           │ Description                                │
 * ├──────────────────┼────────────────────────────────────────────┤
 * │ id               │ UUID primary key                           │
 * │ title            │ Book title                                 │
 * │ author           │ Author name(s) — stored as comma-separated │
 * │ isbn             │ ISBN-13 or ISBN-10 (unique)                │
 * │ category_id      │ FK → categories.id                        │
 * │ publisher        │ Publisher / brand name                     │
 * │ price            │ Price in rupees (e.g. 299.00)              │
 * │ stock            │ How many copies available                  │
 * │ cover_image_url  │ URL to the book cover image                │
 * │ description      │ Short description of the book              │
 * │ published_year   │ Year published                             │
 * │ created_at       │ When this record was created               │
 * └──────────────────┴────────────────────────────────────────────┘
 */
@Entity
@Table(name = "books")
@Getter
@Setter
@NoArgsConstructor
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /** The book's title. Required. */
    @Column(nullable = false)
    private String title;

    /**
     * Author name(s) as a single string.
     * If multiple authors from the seed data, we join them with ", ".
     * Example: "J.K. Rowling" or "Thomas H. Cormen, Charles E. Leiserson"
     */
    @Column(nullable = false)
    private String author;

    /**
     * ISBN-13 or ISBN-10. Unique — no two books share the same ISBN.
     * nullable = true because some older books don't have an ISBN.
     */
    @Column(unique = true)
    private String isbn;

    /**
     * The category this book belongs to.
     *
     * @ManyToOne = many Books can belong to one Category
     * @JoinColumn = the foreign key column in the books table is "category_id"
     * fetch = LAZY means the category is only loaded from the DB when we
     * actually access it — saves unnecessary database calls.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /**
     * Publisher / brand name (e.g. "Penguin Books", "O'Reilly Media").
     * Used for the "Browse by brand" feature (BR-003).
     */
    private String publisher;

    /**
     * Price in Indian Rupees.
     * BigDecimal is used instead of double/float for money
     * because it avoids floating-point rounding errors.
     * Example: 299.00, 549.50
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * How many copies are currently in stock.
     * stock = 0 means "out of stock" — shown differently in the UI.
     * FS-002 AC-5: unavailable books are indicated as such.
     */
    @Column(nullable = false)
    private int stock = 0;

    /** URL of the book cover image (from Open Library's cover service). */
    @Column(name = "cover_image_url")
    private String coverImageUrl;

    /** Short description of the book. Can be long — stored as TEXT in DB. */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Year the book was first published.
     * Stored as an integer (e.g. 2008, 1997).
     */
    @Column(name = "published_year")
    private Integer publishedYear;

    /**
     * When this book record was added to our database.
     * Used by the recommendations engine to find "newest additions" (D-010).
     * updatable = false → never changes after first save.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /**
     * Convenience method: is this book currently available?
     * Returns true if stock > 0.
     * Used in API responses to show availability status (FS-002 AC-5).
     *
     * @Transient = this field is NOT stored in the database.
     * It is computed from `stock` and included in API responses.
     */
    @Transient
    public boolean isAvailable() {
        return stock > 0;
    }
}
