package com.bookstore.service;

import com.bookstore.dto.book.BookDetailDto;
import com.bookstore.dto.book.BookSummaryDto;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.model.Book;
import com.bookstore.model.Category;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * ============================================================
 * BookService — Business Logic for the Book Catalogue
 * ============================================================
 *
 * WHAT THIS DOES:
 * Contains all business logic for:
 *   - Listing books with search and filters (FS-004)
 *   - Getting a single book's full details (FS-003)
 *   - Getting all categories (FS-002)
 *
 * RULE: Services talk to repositories and return DTOs to controllers.
 *       They never talk to other controllers, never return entities directly.
 *
 * @Transactional(readOnly = true):
 * Most methods here only READ data — they don't change anything.
 * readOnly = true is a performance hint to the database:
 * "this transaction won't modify anything, so you can skip some locking overhead".
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;

    /**
     * Get a paginated, filtered list of books.
     * Supports: free-text search, category filter, publisher filter,
     *           price range, availability filter (FS-004).
     *
     * All parameters are optional (null = no filter applied).
     *
     * @param search     - search term for title/author/category/publisher
     * @param categoryId - filter by this category UUID
     * @param publisher  - filter by publisher name
     * @param minPrice   - minimum price
     * @param maxPrice   - maximum price
     * @param available  - true = in-stock only
     * @param page       - page index (0-based)
     * @param size       - number of books per page (max 100)
     * @return paginated page of BookSummaryDto objects
     */
    public Page<BookSummaryDto> getBooks(
            String search,
            UUID categoryId,
            String publisher,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean available,
            int page,
            int size) {

        // Cap page size at 100 to prevent accidental huge queries
        int safeSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, safeSize);

        // Run the query — empty string treated same as null (no search)
        String searchTerm = (search != null && !search.isBlank()) ? search.trim() : null;

        return bookRepository
                .findWithFilters(searchTerm, categoryId, publisher, minPrice, maxPrice, available, pageable)
                .map(this::toSummaryDto);  // Convert each Book entity → BookSummaryDto
    }

    /**
     * Get full details for one book, including related books (FS-003).
     *
     * @param bookId - the UUID of the book to fetch
     * @return BookDetailDto with all fields + relatedBooks
     * @throws ResourceNotFoundException if no book with this ID exists
     */
    public BookDetailDto getBookById(UUID bookId) {
        // Find the book or throw 404 if it doesn't exist
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + bookId));

        // Find up to 6 related books in the same category (D-006)
        // PageRequest.of(0, 6) = first page, 6 results max
        List<Book> relatedBooks = bookRepository.findRelatedBooks(
                book.getCategory().getId(),
                book.getId(),
                PageRequest.of(0, 6)
        );

        return toDetailDto(book, relatedBooks);
    }

    /**
     * Get all categories (used to populate the category sidebar in the catalogue).
     *
     * @return list of all Category entities
     */
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }


    // =========================================================
    // PRIVATE HELPERS — convert entities to DTOs
    // =========================================================

    /**
     * Convert a Book entity into a BookSummaryDto.
     * Called for every book in a list/search result.
     *
     * Why a separate method?
     * If we ever change the DTO shape, we only change it here — not in
     * every place that returns a list of books.
     */
    private BookSummaryDto toSummaryDto(Book book) {
        return new BookSummaryDto(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getPublisher(),
                new BookSummaryDto.CategoryInfo(
                        book.getCategory().getId(),
                        book.getCategory().getName()
                ),
                book.getPrice(),
                book.isAvailable(),
                book.getCoverImageUrl(),
                book.getPublishedYear()
        );
    }

    /**
     * Convert a Book entity + list of related books into a BookDetailDto.
     * Called only for the single-book detail endpoint.
     */
    private BookDetailDto toDetailDto(Book book, List<Book> related) {
        return new BookDetailDto(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getIsbn(),
                book.getDescription(),
                book.getPrice(),
                book.isAvailable(),
                book.getCoverImageUrl(),
                book.getPublishedYear(),
                book.getPublisher(),
                new BookDetailDto.CategoryInfo(
                        book.getCategory().getId(),
                        book.getCategory().getName(),
                        book.getCategory().getDeliveryOffsetDays()
                ),
                book.getCategory().getDeliveryOffsetDays(), // tentativeDeliveryDays
                related.stream().map(this::toSummaryDto).toList()
        );
    }
}
