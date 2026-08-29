package com.bookstore.controller;

import com.bookstore.dto.book.BookDetailDto;
import com.bookstore.dto.book.BookSummaryDto;
import com.bookstore.model.Category;
import com.bookstore.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * ============================================================
 * BookController — HTTP Endpoints for the Book Catalogue
 * ============================================================
 *
 * WHAT THIS IS:
 * Handles all HTTP requests related to books and categories.
 * All endpoints here are PUBLIC — no login required (guests can browse).
 * This is configured in SecurityConfig with .permitAll() for GET /api/books/**
 *
 * ENDPOINTS:
 *   GET /api/books                 → list/search/filter books (paginated)
 *   GET /api/books/{id}            → single book detail + related books
 *   GET /api/categories            → all categories (for sidebar navigation)
 *
 * HOW QUERY PARAMETERS WORK:
 * When the frontend calls GET /api/books?search=harry&minPrice=100
 * Spring automatically reads those values and passes them to the method
 * as parameters using @RequestParam.
 *
 * @RequestParam(required = false) means the parameter is optional —
 * if not provided, the value is null.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    /**
     * GET /api/books
     *
     * Returns a paginated list of books with optional search and filters.
     * ALL parameters are optional — calling with no params returns all books.
     *
     * EXAMPLE CALLS:
     *   GET /api/books                              → all books, page 0
     *   GET /api/books?search=harry                 → search for "harry"
     *   GET /api/books?categoryId=xxx&available=true → in-stock Fiction books
     *   GET /api/books?minPrice=100&maxPrice=500     → books ₹100-₹500
     *   GET /api/books?page=1&size=10               → second page, 10 per page
     *
     * RESPONSE 200:
     * {
     *   "content": [ ...BookSummaryDto objects... ],
     *   "page": 0,
     *   "size": 20,
     *   "totalElements": 115,
     *   "totalPages": 6
     * }
     */
    @GetMapping("/books")
    public ResponseEntity<Page<BookSummaryDto>> getBooks(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String publisher,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean available,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<BookSummaryDto> books = bookService.getBooks(
                search, categoryId, publisher, minPrice, maxPrice, available, page, size
        );
        return ResponseEntity.ok(books);
    }

    /**
     * GET /api/books/{id}
     *
     * Returns full details for one book, including related books.
     *
     * EXAMPLE CALL:
     *   GET /api/books/550e8400-e29b-41d4-a716-446655440000
     *
     * RESPONSE 200: BookDetailDto (see BookDetailDto.java for shape)
     * RESPONSE 404: if the book ID doesn't exist in the database
     *
     * @PathVariable reads the {id} part from the URL path.
     */
    @GetMapping("/books/{id}")
    public ResponseEntity<BookDetailDto> getBookById(@PathVariable UUID id) {
        BookDetailDto book = bookService.getBookById(id);
        return ResponseEntity.ok(book);
    }

    /**
     * GET /api/categories
     *
     * Returns all book categories.
     * Used by the frontend to build the category sidebar/navigation.
     *
     * RESPONSE 200:
     * [
     *   { "id": "...", "name": "Fiction",     "deliveryOffsetDays": 3 },
     *   { "id": "...", "name": "Technology",  "deliveryOffsetDays": 5 },
     *   ...
     * ]
     */
    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(bookService.getAllCategories());
    }
}
