package com.bookstore.service;

import com.bookstore.dto.book.BookDetailDto;
import com.bookstore.dto.book.BookSummaryDto;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.model.Book;
import com.bookstore.model.Category;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.BookSpecification;
import com.bookstore.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ============================================================
 * BookServiceTest — Unit Tests for Book Catalogue Logic
 * ============================================================
 *
 * WHAT WE ARE TESTING:
 *   getBooks()    → returns a paginated page of BookSummaryDto
 *   getBookById() → returns full detail DTO, or throws 404
 *
 * KEY SCENARIOS:
 *   - No filters: all books returned (pagination respected)
 *   - With filters: specification is built and passed correctly
 *   - Book not found → ResourceNotFoundException (404)
 *   - Related books are included in the detail response
 *
 * IMPORTANT NOTE ON MOCKING SPECIFICATIONS:
 * BookService calls bookRepository.findAll(spec, pageable).
 * We cannot match the exact Specification lambda because each call
 * creates a new lambda instance. So we use any(Specification.class)
 * to match any specification — we test the specification itself
 * in BookSpecificationTest.
 */
@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock private BookRepository bookRepository;
    @Mock private CategoryRepository categoryRepository;

    @InjectMocks
    private BookService bookService;

    // ── Reusable test fixtures ──
    private Category fiction;
    private Book book1;
    private Book book2;

    @BeforeEach
    void setUp() {
        fiction = new Category("Fiction", 3);
        setId(fiction, UUID.randomUUID());

        book1 = buildBook("Clean Code", "Robert Martin", new BigDecimal("799.00"), fiction, 5);
        book2 = buildBook("The Pragmatic Programmer", "David Thomas", new BigDecimal("649.00"), fiction, 3);
    }

    // ─────────────────────────────────────────────────────────
    // getBooks()
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getBooks: returns paginated BookSummaryDto when no filters applied")
    void getBooks_noFilters_returnsMappedPage() {
        // Arrange: repository returns a page with two books
        Page<Book> fakePage = new PageImpl<>(List.of(book1, book2), PageRequest.of(0, 20), 2);
        when(bookRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(fakePage);

        // Act
        Page<BookSummaryDto> result = bookService.getBooks(null, null, null, null, null, null, 0, 20);

        // Assert: two items, correct titles
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(BookSummaryDto::title)
                .containsExactly("Clean Code", "The Pragmatic Programmer");
    }

    @Test
    @DisplayName("getBooks: blank search is normalised to null before passing to spec")
    void getBooks_blankSearch_treatedAsNoFilter() {
        Page<Book> fakePage = new PageImpl<>(List.of(book1), PageRequest.of(0, 20), 1);
        when(bookRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(fakePage);

        // A blank/whitespace-only search should behave the same as null
        Page<BookSummaryDto> result = bookService.getBooks("   ", null, null, null, null, null, 0, 20);

        assertThat(result.getContent()).hasSize(1);
        // findAll must still be called once (spec is built even with null search)
        verify(bookRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("getBooks: page size is capped at 100")
    void getBooks_pageSizeCappedAt100() {
        Page<Book> fakePage = new PageImpl<>(List.of(), PageRequest.of(0, 100), 0);
        when(bookRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(fakePage);

        // Request 9999 books — should be capped to 100 internally
        bookService.getBooks(null, null, null, null, null, null, 0, 9999);

        // Capture the pageable that was actually passed to the repository
        var pageableCaptor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(bookRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("getBooks: returns empty page when no books match")
    void getBooks_noMatches_returnsEmptyPage() {
        Page<Book> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(bookRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        Page<BookSummaryDto> result = bookService.getBooks("nonexistent", null, null, null, null, null, 0, 20);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    // ─────────────────────────────────────────────────────────
    // getBookById()
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getBookById: returns BookDetailDto with related books")
    void getBookById_found_returnsDetailDtoWithRelatedBooks() {
        // Arrange
        when(bookRepository.findById(book1.getId())).thenReturn(Optional.of(book1));
        when(bookRepository.findRelatedBooks(eq(fiction.getId()), eq(book1.getId()), any(Pageable.class)))
                .thenReturn(List.of(book2));

        // Act
        BookDetailDto result = bookService.getBookById(book1.getId());

        // Assert: main book fields correct
        assertThat(result.title()).isEqualTo("Clean Code");
        assertThat(result.author()).isEqualTo("Robert Martin");

        // Assert: related books included
        assertThat(result.relatedBooks()).hasSize(1);
        assertThat(result.relatedBooks().get(0).title()).isEqualTo("The Pragmatic Programmer");
    }

    @Test
    @DisplayName("getBookById: throws ResourceNotFoundException when book does not exist")
    void getBookById_notFound_throwsResourceNotFoundException() {
        UUID unknownId = UUID.randomUUID();
        when(bookRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.getBookById(unknownId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(unknownId.toString());
    }

    @Test
    @DisplayName("getBookById: includes category info in the detail response")
    void getBookById_includesCategoryInfo() {
        when(bookRepository.findById(book1.getId())).thenReturn(Optional.of(book1));
        when(bookRepository.findRelatedBooks(any(), any(), any())).thenReturn(List.of());

        BookDetailDto result = bookService.getBookById(book1.getId());

        assertThat(result.category().name()).isEqualTo("Fiction");
        assertThat(result.category().deliveryOffsetDays()).isEqualTo(3);
    }

    // ─────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────

    /** Build a fully populated Book with a random UUID id */
    private Book buildBook(String title, String author, BigDecimal price, Category category, int stock) {
        Book b = new Book();
        setId(b, UUID.randomUUID());
        b.setTitle(title);
        b.setAuthor(author);
        b.setPrice(price);
        b.setCategory(category);
        b.setStock(stock);
        b.setPublisher("Test Publisher");
        b.setPublishedYear(2020);
        return b;
    }

    /** Use reflection to set private/final `id` field that JPA normally sets */
    private void setId(Object entity, UUID id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException("Could not set id on " + entity.getClass().getSimpleName(), e);
        }
    }
}
