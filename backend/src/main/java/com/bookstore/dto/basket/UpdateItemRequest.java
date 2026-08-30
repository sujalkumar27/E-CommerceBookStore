package com.bookstore.dto.basket;

import jakarta.validation.constraints.Min;

/**
 * ============================================================
 * UpdateItemRequest — What the client sends to change quantity
 * ============================================================
 *
 * USED FOR:
 *   PUT /api/basket/items/{itemId}
 *   Body: { "quantity": 3 }
 */
public record UpdateItemRequest(

        @Min(value = 1, message = "Quantity must be at least 1")
        int quantity

) {}
