package com.bookstore.service;

import com.bookstore.dto.cart.AddItemRequest;
import com.bookstore.dto.cart.CartDto;
import com.bookstore.dto.cart.UpdateItemRequest;
import com.bookstore.exception.ForbiddenException;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.model.CartItem;
import com.bookstore.model.Book;
import com.bookstore.model.User;
import com.bookstore.repository.CartRepository;
import com.bookstore.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * ============================================================
 * CartService — Business Logic for the Shopping Cart
 * ============================================================
 *
 * WHAT THIS DOES:
 * Handles all cart operations for a logged-in user:
 *   - Get cart contents
 *   - Add a book (or increase quantity if already in cart)
 *   - Update quantity of an existing item
 *   - Remove an item
 *
 * IMPORTANT RULES:
 *   - Only authenticated users can use the cart (enforced by SecurityConfig)
 *   - If the same book is added twice, quantities are SUMMED (not duplicated)
 *   - A user can only modify THEIR OWN cart items (ownership check)
 *
 * @Transactional = all database operations in a method happen together.
 * If one fails, all are rolled back. Keeps the cart in a consistent state.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final BookRepository bookRepository;

    /**
     * Get the current user's full cart.
     * Read-only — does not modify anything.
     *
     * @param user - the currently logged-in user (from JWT token)
     * @return CartDto with all items and total
     */
    @Transactional(readOnly = true)
    public CartDto getCart(User user) {
        List<CartItem> items = cartRepository.findAllByUserOrderByAddedAtAsc(user);
        return toDto(items);
    }

    /**
     * Add a book to the cart, or increase quantity if already present.
     *
     * LOGIC:
     *   - Find the book by ID (404 if not found)
     *   - Check if this book is already in the user's cart
     *     → YES: add the new quantity to the existing quantity
     *     → NO:  create a new cart item row
     *   - Return the updated full cart
     */
    public CartDto addItem(User user, AddItemRequest request) {
        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Book not found with ID: " + request.bookId()));

        // If book already in cart → sum quantities; else create new item
        cartRepository.findByUserAndBookId(user, book.getId())
                .ifPresentOrElse(
                        existing -> existing.setQuantity(existing.getQuantity() + request.quantity()),
                        () -> cartRepository.save(new CartItem(user, book, request.quantity()))
                );

        return getCart(user);
    }

    /**
     * Update the quantity of an existing cart item.
     * Ownership check: verifies this item belongs to THIS user.
     */
    public CartDto updateItem(User user, UUID itemId, UpdateItemRequest request) {
        CartItem item = cartRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        if (!item.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("This cart item does not belong to you");
        }

        item.setQuantity(request.quantity());
        return getCart(user);
    }

    /**
     * Remove an item from the cart entirely.
     * Ownership check: verifies this item belongs to THIS user.
     */
    public CartDto removeItem(User user, UUID itemId) {
        CartItem item = cartRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        if (!item.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("This cart item does not belong to you");
        }

        cartRepository.delete(item);
        return getCart(user);
    }

    // =========================================================
    // PRIVATE HELPERS
    // =========================================================

    /**
     * Convert a list of CartItem entities into a CartDto.
     * Calculates lineTotal (price × qty) and cartTotal (sum of lineTotals).
     */
    private CartDto toDto(List<CartItem> items) {
        List<CartDto.CartItemDto> itemDtos = items.stream().map(item -> {
            BigDecimal lineTotal = item.getBook().getPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
            return new CartDto.CartItemDto(
                    item.getId(),
                    new CartDto.BookInfo(
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

        BigDecimal cartTotal = itemDtos.stream()
                .map(CartDto.CartItemDto::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartDto(itemDtos, cartTotal);
    }
}
