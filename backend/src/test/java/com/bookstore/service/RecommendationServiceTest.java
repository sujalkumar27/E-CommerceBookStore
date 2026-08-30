package com.bookstore.service;

import com.bookstore.dto.book.BookSummaryDto;
import com.bookstore.model.Book;
import com.bookstore.model.Category;
import com.bookstore.model.User;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.OrderItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ============================================================
 * RecommendationServiceTest — Unit Tests for Recommendation Logic
 * ============================================================
 *
 * WHAT WE ARE TESTING:
 * RecommendationService.getRecommendations() blends 3 signals:
 *   Signal A — books in same categories as past orders
 *   Signal B — books by same authors as past orders
 *   Signal C — newest books (fallback / padding)
 *
 * KEY SCENARIOS:
 *   1. User with NO order history → returns 8 newest books (signal C only)
 *   2. User with order history, enough results from A+B → signal C not used
 *   3. User with order history, A+B gives < 8 → padded with signal C
 *   4. De-duplication: same book appearing in both A and B counted once
 *   5. Maximum of 8 recommendations returned
 */
@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock private BookRepository bookRepository;
    @Mock private OrderItemRepository orderItemRepository;

    @InjectMocks
    private RecommendationService recommendationService;

    private User alice;
    private Category fiction;
    private List<Book> fictionBooks;
    private List<Book> newestBooks;

    @BeforeEach
    void setUp() {
        alice = makeUser("alice@test.com");

        fiction = new Category("Fiction", 3);
        setId(fiction, UUID.randomUUID());

        // 4 fiction books for signal A
        fictionBooks = List.of(
                makeBook("Book A1", "Author A", fiction),
                makeBook("Book A2", "Author A", fiction),
                makeBook("Book A3", "Author B", fiction),
                makeBook("Book A4", "Author B", fiction)
        );

        // 8 books for the "newest" fallback signal C
        newestBooks = List.of(
                makeBook("New 1", "Auth 1", fiction),
                makeBook("New 2", "Auth 2", fiction),
                makeBook("New 3", "Auth 3", fiction),
                makeBook("New 4", "Auth 4", fiction),
                makeBook("New 5", "Auth 5", fiction),
                makeBook("New 6", "Auth 6", fiction),
                makeBook("New 7", "Auth 7", fiction),
                makeBook("New 8", "Auth 8", fiction)
        );
    }

    // ─────────────────────────────────────────────────────────
    // No order history
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("no order history → returns newest books (up to 8)")
    void getRecommendations_noOrderHistory_returnsNewestBooks() {
        // User has never ordered anything
        when(orderItemRepository.findOrderedBookIdsByUser(alice.getId())).thenReturn(List.of());
        // Signal C returns 8 newest books
        when(bookRepository.findNewestExcluding(anyList(), any(Pageable.class)))
                .thenReturn(newestBooks);

        List<BookSummaryDto> result = recommendationService.getRecommendations(alice);

        assertThat(result).hasSize(8);
        // Signals A and B should NOT have been called
        verify(orderItemRepository, never()).findOrderedCategoryIdsByUser(any());
        verify(orderItemRepository, never()).findOrderedAuthorsByUser(any());
    }

    // ─────────────────────────────────────────────────────────
    // Order history present
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("order history present → signal A (category) books included")
    void getRecommendations_withHistory_includesCategoryBooks() {
        List<UUID> orderedBookIds   = List.of(UUID.randomUUID()); // 1 already ordered
        List<UUID> orderedCatIds    = List.of(fiction.getId());
        List<String> orderedAuthors = List.of("Robert Martin");

        when(orderItemRepository.findOrderedBookIdsByUser(alice.getId())).thenReturn(orderedBookIds);
        when(orderItemRepository.findOrderedCategoryIdsByUser(alice.getId())).thenReturn(orderedCatIds);
        when(orderItemRepository.findOrderedAuthorsByUser(alice.getId())).thenReturn(orderedAuthors);

        // Signal A returns 4 books
        when(bookRepository.findByCategoryIdsExcluding(anyList(), anyList(), any(Pageable.class)))
                .thenReturn(fictionBooks);
        // Signal B returns 0 (no books by this author beyond what A found)
        when(bookRepository.findByAuthorsExcluding(anyList(), anyList(), any(Pageable.class)))
                .thenReturn(List.of());
        // Signal C pads the remaining 4 slots
        when(bookRepository.findNewestExcluding(anyList(), any(Pageable.class)))
                .thenReturn(newestBooks.subList(0, 4));

        List<BookSummaryDto> result = recommendationService.getRecommendations(alice);

        // 4 from A + 4 from C = 8
        assertThat(result).hasSize(8);
    }

    @Test
    @DisplayName("signal A + B together give 8 → signal C (newest) not called")
    void getRecommendations_ABFull_doesNotCallSignalC() {
        List<UUID> orderedBookIds   = List.of(UUID.randomUUID());
        List<UUID> orderedCatIds    = List.of(fiction.getId());
        List<String> orderedAuthors = List.of("Robert Martin");

        when(orderItemRepository.findOrderedBookIdsByUser(alice.getId())).thenReturn(orderedBookIds);
        when(orderItemRepository.findOrderedCategoryIdsByUser(alice.getId())).thenReturn(orderedCatIds);
        when(orderItemRepository.findOrderedAuthorsByUser(alice.getId())).thenReturn(orderedAuthors);

        // Signal A gives 4, Signal B gives 4 — total = 8, no padding needed
        when(bookRepository.findByCategoryIdsExcluding(anyList(), anyList(), any(Pageable.class)))
                .thenReturn(fictionBooks); // 4 books
        when(bookRepository.findByAuthorsExcluding(anyList(), anyList(), any(Pageable.class)))
                .thenReturn(List.of(
                        makeBook("B1", "Robert Martin", fiction),
                        makeBook("B2", "Robert Martin", fiction),
                        makeBook("B3", "Robert Martin", fiction),
                        makeBook("B4", "Robert Martin", fiction)
                )); // 4 more books

        List<BookSummaryDto> result = recommendationService.getRecommendations(alice);

        assertThat(result).hasSize(8);
        // Signal C must NOT have been called — we already have 8
        verify(bookRepository, never()).findNewestExcluding(anyList(), any(Pageable.class));
    }

    @Test
    @DisplayName("de-duplication: same book in A and B counted only once")
    void getRecommendations_deduplicatesAcrossSignals() {
        List<UUID> orderedBookIds   = List.of(UUID.randomUUID());
        List<UUID> orderedCatIds    = List.of(fiction.getId());
        List<String> orderedAuthors = List.of("Robert Martin");

        when(orderItemRepository.findOrderedBookIdsByUser(alice.getId())).thenReturn(orderedBookIds);
        when(orderItemRepository.findOrderedCategoryIdsByUser(alice.getId())).thenReturn(orderedCatIds);
        when(orderItemRepository.findOrderedAuthorsByUser(alice.getId())).thenReturn(orderedAuthors);

        // Signal A returns books A1, A2, A3, A4
        when(bookRepository.findByCategoryIdsExcluding(anyList(), anyList(), any(Pageable.class)))
                .thenReturn(fictionBooks);
        // Signal B returns the SAME books (overlap)
        when(bookRepository.findByAuthorsExcluding(anyList(), anyList(), any(Pageable.class)))
                .thenReturn(fictionBooks); // all 4 duplicates
        // Signal C pads the remaining 4 slots (still need 4 more unique books)
        when(bookRepository.findNewestExcluding(anyList(), any(Pageable.class)))
                .thenReturn(newestBooks.subList(0, 4));

        List<BookSummaryDto> result = recommendationService.getRecommendations(alice);

        // A=4 unique, B=0 new (all dups), C=4 padding → total 8, no duplicates
        assertThat(result).hasSize(8);
        // Verify all returned book IDs are unique
        long uniqueCount = result.stream().map(BookSummaryDto::id).distinct().count();
        assertThat(uniqueCount).isEqualTo(8);
    }

    // ─────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────

    private User makeUser(String email) {
        User u = new User("Test User", email, "hash");
        setId(u, UUID.randomUUID());
        return u;
    }

    private Book makeBook(String title, String author, Category category) {
        Book b = new Book();
        setId(b, UUID.randomUUID());
        b.setTitle(title);
        b.setAuthor(author);
        b.setPrice(new BigDecimal("499.00"));
        b.setStock(10);
        b.setCategory(category);
        b.setPublisher("Test Publisher");
        b.setPublishedYear(2022);
        return b;
    }

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

