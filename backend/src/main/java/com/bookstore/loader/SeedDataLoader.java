package com.bookstore.loader;

import com.bookstore.model.Book;
import com.bookstore.model.Category;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.CategoryRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * ============================================================
 * SeedDataLoader — Loads Book Catalogue from seed.json at Startup
 * ============================================================
 *
 * WHAT THIS DOES:
 * Every time the Spring Boot app starts, this class:
 *   1. Reads backend/src/main/resources/data/seed.json
 *   2. Creates missing categories in the database
 *   3. Inserts books that are not already in the database (by ISBN)
 *   4. Skips books that already exist (idempotent — safe to restart)
 *
 * WHY ApplicationRunner?
 * ApplicationRunner is a Spring interface with one method: run().
 * Spring calls run() automatically AFTER the app has fully started
 * (database is connected, tables are created by Hibernate).
 * This is the right place to do "startup setup" work.
 *
 * IDEMPOTENT = safe to run multiple times:
 * If the app restarts, we don't insert duplicate books.
 * We check existsByIsbn() before inserting each book.
 *
 * CATEGORY DELIVERY OFFSETS:
 * These are set per the technical design (D-005):
 *   Fiction      → 3 days
 *   Technology   → 5 days
 *   History      → 5 days
 *   Business     → 5 days
 *   Self-Help    → 5 days
 *   Science      → 5 days
 *   Biography    → 5 days
 *   Philosophy   → 7 days
 *   (default)    → 5 days
 */
@Component
@RequiredArgsConstructor
public class SeedDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDataLoader.class);

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final ObjectMapper objectMapper;  // Spring's built-in JSON parser

    // Read the seed file path from application.yml: app.seed.path
    @Value("${app.seed.path}")
    private Resource seedResource;

    /**
     * Called automatically by Spring after the app starts.
     * Reads seed.json and populates the database.
     *
     * @Transactional = everything happens in one database transaction.
     * If anything fails halfway through, ALL changes are rolled back (not half-inserted).
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {

        // Check if the seed file exists
        if (!seedResource.exists()) {
            log.warn("=======================================================");
            log.warn("seed.json not found at: {}", seedResource.getDescription());
            log.warn("Run the seed script first:  python seed/fetch_books.py");
            log.warn("Then restart the application.");
            log.warn("=======================================================");
            return;
        }

        // Parse the JSON file into a list of maps (each map = one book)
        List<Map<String, Object>> booksJson = objectMapper.readValue(
                seedResource.getInputStream(),
                new TypeReference<>() {}
        );

        log.info("Seed file loaded — {} book records found", booksJson.size());

        int[] categoriesCreated = {0};  // array trick: lets us increment inside a lambda
        int booksInserted       = 0;
        int booksSkipped        = 0;

        for (Map<String, Object> bookData : booksJson) {

            // ---- Step 1: Get or create the category ----
            // The seed JSON has a "category" field like "Fiction", "Technology"
            String categoryName = (String) bookData.get("category");
            if (categoryName == null || categoryName.isBlank()) {
                categoryName = "General";
            }

            // Decide the delivery offset for this category
            final String catName = categoryName;
            int offsetDays = switch (catName.toLowerCase()) {
                case "fiction"     -> 3;
                case "philosophy"  -> 7;
                default            -> 5;
            };

            // Find existing category or create a new one
            Category category = categoryRepository.findByName(categoryName)
                    .orElseGet(() -> {
                        Category newCat = new Category(catName, offsetDays);
                        categoriesCreated[0]++;
                        return categoryRepository.save(newCat);
                    });

            // ---- Step 2: Skip if this book is already in the DB ----
            String isbn = (String) bookData.get("isbn");
            if (isbn != null && bookRepository.existsByIsbn(isbn)) {
                booksSkipped++;
                continue;
            }

            // ---- Step 3: Build the Book entity ----
            Book book = new Book();
            book.setTitle(safeString(bookData.get("title"), "Unknown Title"));
            book.setIsbn(isbn);
            book.setCategory(category);
            book.setDescription(safeString(bookData.get("description"), "No description available."));
            book.setCoverImageUrl(safeString(bookData.get("coverImageUrl"), null));
            book.setPublisher(safeString(bookData.get("publisher"), null));
            book.setPublishedYear(safeInt(bookData.get("publishedDate")));
            book.setStock(safeInt(bookData.get("stockQuantity")));

            // Price: convert from the JSON number to BigDecimal
            Object priceRaw = bookData.get("price");
            BigDecimal price = priceRaw != null
                    ? new BigDecimal(priceRaw.toString())
                    : BigDecimal.valueOf(299.00);
            book.setPrice(price);

            // Author: the seed JSON has "authors" as a list of strings
            // We join them with ", " to store as a single string
            Object authorsRaw = bookData.get("authors");
            if (authorsRaw instanceof List<?> authorsList) {
                String authorStr = authorsList.stream()
                        .map(Object::toString)
                        .limit(5)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("Unknown Author");
                book.setAuthor(authorStr);
            } else {
                book.setAuthor("Unknown Author");
            }

            // ---- Step 4: Save the book ----
            bookRepository.save(book);
            booksInserted++;
        }

        log.info("Seed complete — categories created: {}, books inserted: {}, books skipped (already exist): {}",
                categoriesCreated[0], booksInserted, booksSkipped);

        if (bookRepository.count() == 0) {
            log.warn("Database has 0 books after seeding — something may be wrong.");
        } else {
            log.info("Total books in database: {}", bookRepository.count());
        }
    }

    /**
     * Safely read a string value from the JSON map.
     * Returns the defaultValue if the value is null or blank.
     */
    private String safeString(Object value, String defaultValue) {
        if (value == null) return defaultValue;
        String str = value.toString().trim();
        return str.isEmpty() ? defaultValue : str;
    }

    /**
     * Safely read an integer value from the JSON map.
     * The JSON might store it as a String like "2008" or a number 2008.
     * Returns 0 if the value is null or cannot be parsed.
     */
    private int safeInt(Object value) {
        if (value == null) return 0;
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
