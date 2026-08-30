package com.bookstore.service;

import com.bookstore.dto.book.BookSummaryDto;
import com.bookstore.model.User;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

/**
 * ============================================================
 * RecommendationService — Personalised Book Recommendations
 * ============================================================
 *
 * WHAT THIS DOES:
 * Returns up to 8 book recommendations for a logged-in user.
 * Based on their order history (FS-011, D-010).
 *
 * THE 3 SIGNALS (blended together):
 *
 *   Signal A — SAME_CATEGORY:
 *     "You ordered Fiction books before → here are more Fiction books"
 *     Finds books in the same categories as the user's past orders.
 *
 *   Signal B — SAME_AUTHOR:
 *     "You read J.K. Rowling before → here are more J.K. Rowling books"
 *     Finds books by the same authors as the user's past orders.
 *
 *   Signal C — NEWEST:
 *     "Here are the latest books added to our catalogue"
 *     Used as fallback if user has no order history, or to pad to 8.
 *
 * RULES:
 *   - Books the user already ordered are EXCLUDED
 *   - If user has NO order history → return 8 newest books only
 *   - Maximum 8 recommendations total
 *   - De-duplicated across all signals
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

    private final BookRepository bookRepository;
    private final OrderItemRepository orderItemRepository;   // built in Phase 5 — declared here

    private static final int MAX_RECOMMENDATIONS = 8;

    /**
     * Get personalised recommendations for the logged-in user.
     *
     * @param user - the currently logged-in user
     * @return list of up to 8 BookSummaryDto objects
     */
    public List<BookSummaryDto> getRecommendations(User user) {

        // Step 1: Find all book IDs this user has already ordered
        List<UUID> orderedBookIds = orderItemRepository.findOrderedBookIdsByUser(user.getId());

        // Step 2: If user has never ordered anything → return 8 newest books
        if (orderedBookIds.isEmpty()) {
            return bookRepository
                    .findNewestExcluding(List.of(UUID.randomUUID()), PageRequest.of(0, MAX_RECOMMENDATIONS))
                    .stream().map(this::toSummaryDto).toList();
        }

        // Step 3: Collect signals from past orders
        List<UUID> orderedCategoryIds = orderItemRepository.findOrderedCategoryIdsByUser(user.getId());
        List<String> orderedAuthors   = orderItemRepository.findOrderedAuthorsByUser(user.getId());

        // Step 4: Use LinkedHashMap to collect results — maintains insertion order + deduplicates by book ID
        // Key = book ID, Value = BookSummaryDto
        LinkedHashMap<UUID, BookSummaryDto> results = new LinkedHashMap<>();

        // Signal A — same category books (up to 4)
        if (!orderedCategoryIds.isEmpty()) {
            bookRepository.findByCategoryIdsExcluding(
                    orderedCategoryIds, orderedBookIds, PageRequest.of(0, 4)
            ).forEach(b -> results.putIfAbsent(b.getId(), toSummaryDto(b)));
        }

        // Signal B — same author books (up to 4, fill remaining slots)
        if (!orderedAuthors.isEmpty() && results.size() < MAX_RECOMMENDATIONS) {
            bookRepository.findByAuthorsExcluding(
                    orderedAuthors, orderedBookIds, PageRequest.of(0, 4)
            ).forEach(b -> results.putIfAbsent(b.getId(), toSummaryDto(b)));
        }

        // Signal C — newest books (pad up to 8 if not enough from A+B)
        if (results.size() < MAX_RECOMMENDATIONS) {
            // Exclude already-ordered books AND books already in results
            List<UUID> alreadySeen = new ArrayList<>(orderedBookIds);
            alreadySeen.addAll(results.keySet());

            bookRepository.findNewestExcluding(
                    alreadySeen, PageRequest.of(0, MAX_RECOMMENDATIONS - results.size())
            ).forEach(b -> results.putIfAbsent(b.getId(), toSummaryDto(b)));
        }

        return new ArrayList<>(results.values());
    }

    // =========================================================
    // PRIVATE HELPER
    // =========================================================

    /**
     * Convert a Book entity to a BookSummaryDto.
     * Same mapping as BookService — kept here to avoid a circular dependency.
     */
    private BookSummaryDto toSummaryDto(com.bookstore.model.Book book) {
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
}
