package com.bookstore.dto.book;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * ============================================================
 * BookDetailDto — Full book info for the product detail page
 * ============================================================
 *
 * WHAT THIS IS:
 * The full response shape for a single book.
 * Used when a customer clicks on a book to view its details.
 *
 * EXTRA FIELDS COMPARED TO BookSummaryDto:
 * - isbn              → for display on detail page
 * - description       → full book description
 * - tentativeDeliveryDays → how many days until delivery (from category)
 * - relatedBooks      → up to 6 books in the same category
 *
 * WHERE THIS IS USED:
 *   GET /api/books/{id} → returns one BookDetailDto
 *
 * EXAMPLE JSON:
 * {
 *   "id": "550e8400-...",
 *   "title": "Clean Code",
 *   "author": "Robert C. Martin",
 *   "isbn": "9780132350884",
 *   "description": "A handbook of agile software craftsmanship...",
 *   "price": 799.00,
 *   "available": true,
 *   "coverImageUrl": "https://...",
 *   "publishedYear": 2008,
 *   "category": { "id": "...", "name": "Technology", "deliveryOffsetDays": 5 },
 *   "tentativeDeliveryDays": 5,
 *   "relatedBooks": [ ...up to 6 BookSummaryDto objects... ]
 * }
 */
public record BookDetailDto(
        UUID id,
        String title,
        String author,
        String isbn,
        String description,
        BigDecimal price,
        boolean available,
        String coverImageUrl,
        Integer publishedYear,
        String publisher,
        CategoryInfo category,
        int tentativeDeliveryDays,            // Convenience field for the UI (= category.deliveryOffsetDays)
        List<BookSummaryDto> relatedBooks     // Up to 6 books in the same category
) {
    /**
     * Category info with deliveryOffsetDays included (needed for delivery date display).
     */
    public record CategoryInfo(UUID id, String name, int deliveryOffsetDays) {}
}
