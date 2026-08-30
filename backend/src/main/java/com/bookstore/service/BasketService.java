package com.bookstore.service;

import com.bookstore.dto.basket.AddItemRequest;
import com.bookstore.dto.basket.BasketDto;
import com.bookstore.dto.basket.UpdateItemRequest;
import com.bookstore.exception.ForbiddenException;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.model.BasketItem;
import com.bookstore.model.Book;
import com.bookstore.model.User;
import com.bookstore.repository.BasketRepository;
import com.bookstore.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * ============================================================
 * BasketService — Business Logic for the Shopping Basket
 * ============================================================
 *
 * WHAT THIS DOES:
 * Handles all basket operations for a logged-in user:
 *   - Get basket contents
 *   - Add a book (or increase quantity if already in basket)
 *   - Update quantity of an existing item
 *   - Remove an item
 *
 * IMPORTANT RULES:
 *   - Only authenticated users can use the basket (enforced by SecurityConfig)
 *   - If the same book is added twice, quantities are SUMMED (not duplicated)
 *   - A user can only modify THEIR OWN basket items (ownership check)
 *
 * @Transactional = all database operations in a method happen together.
 * If one fails, all are rolled back. Keeps the basket in a consistent state.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class BasketService {

    private final BasketRepository basketRepository;
    private final BookRepository bookRepository;

    /**
     * Get the current user's full basket.
     * Read-only — does not modify anything.
     *
     * @param user - the currently logged-in user (from JWT token)
     * @return BasketDto with all items and total
     */
    @Transactional(readOnly = true)
    public BasketDto getBasket(User user) {
        List<BasketItem> items = basketRepository.findAllByUserOrderByAddedAtAsc(user);
        return toDto(items);
    }

    /**
     * Add a book to the basket, or increase quantity if already present.
     *
     * LOGIC:
     *   - Find the book by ID (404 if not found)
     *   - Check if this book is already in the user's basket
     *     → YES: add the new quantity to the existing quantity
     *     → NO:  create a new basket item row
     *   - Return the updated full basket
     *
     * @param user    - the currently logged-in user
     * @param request - contains bookId and quantity
     * @return updated BasketDto
     */
    public BasketDto addItem(User user, AddItemRequest request) {

        // Find the book — throw 404 if it doesn't exist
        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Book not found with ID: " + request.bookId()));

        // Check if this book is already in the user's basket
        basketRepository.findByUserAndBookId(user, book.getId())
                .ifPresentOrElse(
                        // Book already in basket → just add to existing quantity
                        existing -> existing.setQuantity(existing.getQuantity() + request.quantity()),

                        // Book not in basket → create a new basket item
                        () -> basketRepository.save(new BasketItem(user, book, request.quantity()))
                );

        return getBasket(user);
    }

    /**
     * Update the quantity of an existing basket item.
     *
     * OWNERSHIP CHECK:
     * We verify the basket item belongs to THIS user.
     * Without this check, user A could change user B's basket quantity.
     *
     * @param user    - the currently logged-in user
     * @param itemId  - the basket item UUID to update
     * @param request - contains the new quantity
     * @return updated BasketDto
     */
    public BasketDto updateItem(User user, UUID itemId, UpdateItemRequest request) {

        // Find the basket item — throw 404 if not found
        BasketItem item = basketRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Basket item not found"));

        // Ownership check — is this item actually in THIS user's basket?
        if (!item.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("This basket item does not belong to you");
        }

        // Update the quantity
        item.setQuantity(request.quantity());

        return getBasket(user);
    }

    /**
     * Remove an item from the basket entirely.
     *
     * OWNERSHIP CHECK:
     * Same as updateItem — verify this item belongs to this user.
     *
     * @param user   - the currently logged-in user
     * @param itemId - the basket item UUID to remove
     * @return updated BasketDto (without the removed item)
     */
    public BasketDto removeItem(User user, UUID itemId) {

        BasketItem item = basketRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Basket item not found"));

        // Ownership check
        if (!item.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("This basket item does not belong to you");
        }

        basketRepository.delete(item);

        return getBasket(user);
    }


    // =========================================================
    // PRIVATE HELPERS
    // =========================================================

    /**
     * Convert a list of BasketItem entities into a BasketDto.
     *
     * Calculates:
     *   lineTotal    = book.price × quantity  (for each item)
     *   basketTotal  = sum of all lineTotals
     */
    private BasketDto toDto(List<BasketItem> items) {

        List<BasketDto.BasketItemDto> itemDtos = items.stream().map(item -> {

            // lineTotal = price × quantity
            BigDecimal lineTotal = item.getBook().getPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));

            return new BasketDto.BasketItemDto(
                    item.getId(),
                    new BasketDto.BookInfo(
                            item.getBook().getId(),
                            item.getBook().getTitle(),
                            item.getBook().getAuthor(),
                            item.getBook().getPrice(),
                            item.getBook().isAvailable(),
                            item.getBook().getCoverImageUrl()
                    ),
                    item.getQuantity(),
                    lineTotal
            );
        }).toList();

        // basketTotal = sum of all lineTotals
        BigDecimal basketTotal = itemDtos.stream()
                .map(BasketDto.BasketItemDto::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new BasketDto(itemDtos, basketTotal);
    }
}
