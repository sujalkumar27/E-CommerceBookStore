package com.bookstore.dto.basket;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * ============================================================
 * AddItemRequest — What the client sends to add a book to basket
 * ============================================================
 *
 * USED FOR:
 *   POST /api/basket/items
 *   Body: { "bookId": "uuid-of-book", "quantity": 2 }
 */
public record AddItemRequest(

        @NotNull(message = "Book ID is required")
        UUID bookId,

        @Min(value = 1, message = "Quantity must be at least 1")
        int quantity

) {}
