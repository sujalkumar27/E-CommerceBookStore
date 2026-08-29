package com.bookstore.dto.book;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * ============================================================
 * BookSummaryDto — Book info shown in catalogue list / search results
 * ============================================================
 *
 * WHAT THIS IS:
 * A DTO (Data Transfer Object) — the shape of data we send to the
 * frontend when showing a LIST of books (e.g. the catalogue page,
 * search results).
 *
 * WHY NOT SEND THE FULL Book ENTITY?
 * The full entity has fields we don't want in a list view (description,
 * createdAt, etc.) and internal fields. DTOs give us control over exactly
 * what is exposed in the API.
 *
 * WHERE THIS IS USED:
 *   GET /api/books          → returns Page<BookSummaryDto>
 *   GET /api/books/{id}     → includes List<BookSummaryDto> as relatedBooks
 *   GET /api/recommendations → returns List<BookSummaryDto>
 *
 * EXAMPLE JSON:
 * {
 *   "id": "550e8400-...",
 *   "title": "Harry Potter",
 *   "author": "J.K. Rowling",
 *   "publisher": "Bloomsbury",
 *   "category": { "id": "...", "name": "Fiction" },
 *   "price": 499.00,
 *   "available": true,
 *   "coverImageUrl": "https://covers.openlibrary.org/b/id/12345-M.jpg",
 *   "publishedYear": 1997
 * }
 */
public record BookSummaryDto(
        UUID id,
        String title,
        String author,
        String publisher,
        CategoryInfo category,
        BigDecimal price,
        boolean available,
        String coverImageUrl,
        Integer publishedYear
) {
    /**
     * Nested category info included in every book summary.
     * Gives the frontend the category name for display and the ID for filtering.
     */
    public record CategoryInfo(UUID id, String name) {}
}
